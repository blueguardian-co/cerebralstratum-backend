# ADR-0002: Observability and Fleet Dashboard Embedding

| Field             | Value                                                                  |
|-------------------|------------------------------------------------------------------------|
| **Status**        | Accepted                                                               |
| **Date**          | 2026-03-06                                                             |
| **Author**        | Platform Engineering                                                   |
| **Migrated from** | Project-wide ADR-004                                                   |

---

## 1. Context

The GPS tracking platform receives periodic status messages from field devices. These messages are persisted in PostgreSQL using a schema-per-tenant isolation model. Two requirements have emerged:

- Fleet health metrics must be ingested into Grafana Cloud for internal operations monitoring, alerting, and capacity planning.
- Enterprise customers require access to real-time fleet status dashboards scoped strictly to their own devices, surfaced via the platform's web, mobile, and desktop clients (built with Kotlin Multiplatform).

Key constraints driving this design:

- **Data sovereignty.** Grafana Cloud is used for dashboards and alerting only — not as a data store. Raw device data never leaves the operator's servers.
- **No duplicate write paths.** Status messages are written once (to PostgreSQL). Observability is derived from that single source of truth.
- **Tenant isolation at every layer.** A customer must never be able to observe another tenant's data, even through URL manipulation.
- **Clients are the only customer surface.** Customers do not interact with Grafana directly. The KMP applications are the sole interface.

---

## 2. Decision

Adopt a **single-write, scrape-for-metrics** architecture with **server-side embed URL generation** for customer-facing dashboards.

### 2.1 Metrics Ingestion Flow

Quarkus is the sole writer. When a device status message arrives, the backend persists it to the tenant's PostgreSQL schema. No secondary write path exists.

Grafana Alloy runs as a sidecar alongside the Quarkus backend. It scrapes a set of PostgreSQL views on a 30-second interval using a dedicated read-only monitoring role, and forwards the resulting time-series to Grafana Cloud via Prometheus `remote_write`. The views act as the metric contract between the persistence layer and the observability layer.

```
Device
  └─► Quarkus (ingest + validate)
        └─► PostgreSQL (schema-per-tenant, persistent)
                ▲
          Grafana Alloy (read-only scrape, 30s)
                └─► Grafana Cloud (Prometheus / Mimir)
                      └─► Grafana Dashboards + Alerting
```

### 2.2 PostgreSQL Monitoring Views

A set of views are created in each tenant schema as part of tenant provisioning. These aggregate raw status rows into gauge-friendly metrics. Alloy maps each view row to a labelled Prometheus gauge.

| View                     | Granularity               | Metrics Emitted                                                                                  |
|--------------------------|---------------------------|--------------------------------------------------------------------------------------------------|
| `device_status_summary`  | Per device                | `fleet_device_online`, `fleet_device_last_seen_seconds`, `fleet_device_battery_level`            |
| `fleet_health_by_tenant` | Per tenant                | `fleet_devices_total`, `fleet_devices_online`, `fleet_devices_offline`, `fleet_devices_warning`  |
| `message_ingestion_rate` | Per tenant (5-min window) | `fleet_messages_per_minute`                                                                      |
| `stale_devices`          | Per device                | `fleet_device_stale` (1 if `last_seen > threshold`)                                              |

Example DDL for the core per-device view:

```sql
-- Executed via tenant provisioning migration in each tenant schema
CREATE OR REPLACE VIEW device_status_summary AS
SELECT
    d.device_id,
    d.tenant_id,
    d.label,
    CASE WHEN s.reported_at > NOW() - INTERVAL '5 minutes' THEN 1 ELSE 0 END AS is_online,
    EXTRACT(EPOCH FROM (NOW() - s.reported_at))::INT                          AS last_seen_seconds,
    s.battery_pct,
    s.status_code
FROM devices d
LEFT JOIN LATERAL (
    SELECT reported_at, battery_pct, status_code
    FROM device_status_log
    WHERE device_id = d.device_id
    ORDER BY reported_at DESC
    LIMIT 1
) s ON true;
```

The `LATERAL` join ensures only the most recent status row per device is read, keeping scrape query cost O(devices) rather than O(status_log rows).

### 2.3 Grafana Alloy Configuration

Alloy is deployed as a single binary sidecar. It requires only a read-only Postgres connection and a Grafana Cloud `remote_write` endpoint. Configuration uses three River components:

1. `prometheus.exporter.postgres` — points at the monitoring role, runs custom queries against the views.
2. `prometheus.scrape` — reads from the exporter and forwards downstream.
3. `prometheus.remote_write` — forwards to Grafana Cloud Mimir.

```hcl
prometheus.exporter.postgres "fleet_db" {
  data_source_names = [env("MONITORING_DB_URL")]
  query_path        = "/etc/alloy/queries.yaml"
}

prometheus.scrape "fleet_metrics" {
  targets         = prometheus.exporter.postgres.fleet_db.targets
  forward_to      = [prometheus.remote_write.grafana_cloud.receiver]
  scrape_interval = "30s"
}

prometheus.remote_write "grafana_cloud" {
  endpoint {
    url = env("GRAFANA_CLOUD_REMOTE_WRITE_URL")
    basic_auth {
      username = env("GRAFANA_CLOUD_USER")
      password = env("GRAFANA_CLOUD_API_KEY")
    }
  }
}
```

`queries.yaml` maps each view to a named metric, promoting the `tenant_id` column to a Prometheus label.

### 2.4 Label Discipline

All metrics emitted by Alloy must carry the `tenant_id` label. This is non-negotiable — it is the only mechanism by which Grafana dashboards can filter to a single tenant's data. The label is sourced from the `tenant_id` column present in every monitoring view.

Standard label set for all fleet metrics:

```
fleet_device_last_seen_seconds{
  tenant_id = "acme-corp",
  device_id = "dev-00a1b2c3",
  label     = "Vehicle 42",
}
```

### 2.5 Customer Dashboard Embedding

Enterprise customers access fleet dashboards through the KMP client applications. Customers never interact with Grafana directly. The tenant boundary is enforced server-side on every dashboard request.

#### Embed URL Generation

The Quarkus backend exposes a protected endpoint that, given an authenticated session with a resolved `tenant_id`, generates a scoped Grafana embed URL. The URL encodes `tenant_id` as a locked template variable (`var-tenant_id`), preventing client-side manipulation. Kiosk mode strips Grafana chrome.

```kotlin
@GET
@Path("/dashboard/fleet")
@RolesAllowed("CUSTOMER")
fun getFleetDashboardUrl(@Context ctx: SecurityContext): DashboardUrlResponse {

    val tenantId     = resolveTenantId(ctx)
    val grafanaToken = grafanaTokenStore.getToken(tenantId)

    val url = grafanaEmbedBuilder
        .dashboard("fleet-overview")
        .lockVariable("tenant_id", tenantId)
        .kioskMode()
        .signedToken(grafanaToken)
        .expiresIn(Duration.ofMinutes(60))
        .build()

    return DashboardUrlResponse(url)
}
```

Resulting embed URL structure:

```
https://<org>.grafana.net/d/<uid>/fleet-overview
  ?kiosk=tv
  &var-tenant_id=<tenant-id>     ← locked, not editable by client
  &from=now-24h&to=now
  &auth_token=<scoped-sa-token>
```

#### KMP Client Integration

The KMP shared module calls the embed URL endpoint and passes the result to a platform-specific `WebView` via `expect`/`actual`. The shared module owns the API call and URL lifecycle; the platform layer handles rendering.

```kotlin
// commonMain — shared ViewModel
class DashboardViewModel(private val api: DashboardApi) : ViewModel() {

    val embedUrl: StateFlow<String?> = flow {
        emit(api.getFleetDashboardUrl().url)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}

// commonMain — expect declaration
@Composable
expect fun FleetDashboardView(url: String)
```

```kotlin
// androidMain
@Composable
actual fun FleetDashboardView(url: String) {
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            loadUrl(url)
        }
    })
}

// iosMain
@Composable
actual fun FleetDashboardView(url: String) {
    UIKitView(factory = {
        WKWebView().also { it.load(URLRequest(URL(string = url)!!)) }
    })
}

// desktopMain (Compose for Desktop)
@Composable
actual fun FleetDashboardView(url: String) {
    JcefBrowser(url = url)
}
```

#### Grafana Service Accounts

Each tenant is provisioned with a dedicated Grafana Service Account, scoped to read-only access on the fleet datasource. Tokens are rotated on each embed URL generation. A Quarkus background service handles SA provisioning at tenant onboarding and token rotation transparently.

---

## 3. Consequences

### Positive

- **Single write path.** PostgreSQL is the sole data store. No metric buffer, no duplicate writes, no drift between storage and observability.
- **Schema as contract.** The PostgreSQL views define precisely what is observable. Metric changes are schema migrations — reviewed, versioned, and tested.
- **Tenant isolation by construction.** The `tenant_id` label is derived from the data itself, not injected at the application layer, reducing misconfiguration risk.
- **Thin clients.** KMP clients fetch a URL and render a WebView. Dashboard logic, data access, and auth are entirely server-side.
- **Invisible token rotation.** Tokens expire on the backend; clients simply request a fresh URL on next load.

### Trade-offs & Limitations

- **30-second scrape lag.** Fleet status in Grafana lags up to 30 seconds behind the database. Acceptable for health monitoring, but unsuitable for hard real-time alerting. If sub-minute alerting is required, a targeted Quarkus → Micrometer push path for specific alert-grade metrics can be added without changing this architecture.
- **Grafana Service Account provisioning overhead.** Each tenant requires a Grafana SA. A pool/rotation model may be needed above ~500 tenants.
- **CEF dependency on desktop.** `JcefBrowser` adds ~80 MB to the desktop distribution. A native chart implementation may be preferable for the desktop client long-term.
- **Monitoring role spans tenant schemas.** The read-only monitoring role requires `SELECT` on views in all tenant schemas. This is a deliberate, narrow exception to schema isolation — the role has no write access and cannot query raw tables, only the defined views.

---

## 4. Rejected Alternatives

| Alternative                               | Reason Considered                        | Reason Rejected                                                                                                                                                   |
|-------------------------------------------|------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Quarkus → Micrometer push for all metrics | Real-time, no polling lag                | Duplicates the write path. Fleet state already lives in Postgres; pushing it again creates two sources of truth and couples the ingest hot path to observability.  |
| Separate Grafana org per tenant           | Strongest isolation primitive in Grafana | Operationally unsustainable. Dashboard updates require propagation across N orgs. Does not compose with the KMP embedding model.                                   |
| Direct Grafana access for customers       | Removes embed complexity                 | Exposes the Grafana UI to customers; requires per-customer Grafana accounts; breaks white-label requirement; increases cross-tenant data leak surface.             |
| Native KMP charts (no embed)             | No WebView dependency, fully native UX   | Significant cost to replicate Grafana's time-series, alerting annotation, and drill-down capabilities. Deferred — may revisit for a simplified mobile-only view.   |

---

## 5. Open Questions

- **Alert routing.** Grafana Cloud can evaluate alerting rules against the ingested metrics. Notification dispatch design is tracked separately.
- **Alloy deployment model.** Co-located sidecar (current assumption) vs. dedicated observability pod. Revisit at first production deployment.
- **Scrape interval tuning.** 30 seconds is the initial default. Profile `fleet_health_by_tenant` query cost at scale before going to production.
- **Dashboard version management.** Grafana dashboard JSON should be stored in the platform repository (Grafana Terraform provider or dashboard-as-code) to enable review and rollback.

---

## 6. Implementation Checklist

1. Create monitoring PostgreSQL role with `SELECT` on views only — no direct table access.
2. Author and test `device_status_summary`, `fleet_health_by_tenant`, `message_ingestion_rate`, and `stale_devices` views. Add view creation to the tenant provisioning migration.
3. Wire views into `queries.yaml` for Alloy's Postgres exporter. Validate `tenant_id` label is emitted on all metrics.
4. Deploy Grafana Alloy sidecar. Confirm `remote_write` to Grafana Cloud succeeds and metrics appear in Explore.
5. Build parameterised Grafana dashboards with `tenant_id` as a required, non-nullable template variable.
6. Implement Grafana Service Account provisioning in the tenant onboarding flow (Quarkus service + Grafana HTTP API).
7. Implement `/dashboard/fleet` embed URL endpoint in Quarkus with token scoping and kiosk mode parameters.
8. Implement `expect`/`actual` `FleetDashboardView` in KMP for Android, iOS, and Desktop targets.
9. Add integration test: verify that an embed URL generated for tenant A cannot return data for tenant B.
10. Commit dashboard JSON to `/infra/grafana/dashboards/` and wire to the Grafana Terraform provider.
