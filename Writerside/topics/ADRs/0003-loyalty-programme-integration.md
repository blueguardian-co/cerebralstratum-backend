# ADR-0003: Loyalty Programme Integration

| Field             | Value                                    |
|-------------------|------------------------------------------|
| **Status**        | Proposed — Pending Commercial Partnership |
| **Date**          | 2026-04-27                               |
| **Author**        | Alex Henshaw                             |
| **Deciders**      | BlueGuardian Co                          |
| **Migrated from** | Project-wide ADR-009                     |

---

## Context

Subscription churn is one of the most significant risks for a SaaS platform with recurring revenue. Post-launch, BlueGuardian intends to reduce churn by integrating with established consumer loyalty programmes, so that users earn reward points on each successful subscription renewal. This creates a positive re-engagement loop that extends beyond the intrinsic value of the platform itself.

The integration targets the consumer (TrueWard) tier in the first instance, where individual subscribers have personal loyalty memberships. Commercial/B2B tiers (Business, Enterprise) are out of scope for the initial rollout.

Loyalty programme partnerships are not self-service: they require a negotiated commercial agreement with the programme operator before any technical integration is possible. This ADR therefore establishes the **target architecture** and **provider abstraction** to be implemented during the pre-launch build phase, so that when the business development process concludes, integration is a matter of implementing a known interface rather than a design-time decision.

This ADR does not commit to a specific provider, points rate, or integration timeline. Those decisions are dependent on the outcome of commercial negotiations and are tracked separately.

---

## Decision

CEREBRAL STRATUM will implement a **pluggable loyalty provider abstraction** within the Billing & Entitlement Sync service. Points awards will be triggered by successful subscription renewal events originating from Shopify, and dispatched asynchronously using the outbox pattern to ensure renewal processing is never blocked by loyalty award failures.

The system will be region-gated: only users whose billing address region maps to a supported provider will have points awarded. Provider registration in the user profile is opt-in and user-initiated.

The integration will be disabled by default via a feature flag (`LOYALTY_INTEGRATION_ENABLED`) and fully absent from `community` entitlement mode deployments.

The primary target programme for the Australian market is **Flybuys** (Loyalty Pacific Pty Ltd), subject to a commercial partnership agreement.

---

## Provider Abstraction

All loyalty programme integrations implement a common interface, housed in the Billing & Entitlement Sync service:

```java
package co.blueguardian.cerebralstratum.billing.loyalty;

public interface LoyaltyProvider {

    /**
     * Stable identifier for this provider, e.g. "flybuys", "airpoints".
     */
    String getProviderId();

    /**
     * The region this provider is available in.
     */
    Region getSupportedRegion();

    /**
     * Award points for a successful subscription renewal.
     * Implementations must be idempotent — duplicate calls for the same
     * shopifyOrderId must not result in duplicate point awards.
     */
    void awardRenewalPoints(LoyaltyMembership membership, SubscriptionRenewalEvent event);

    /**
     * Validate that an external member token/number is well-formed
     * before persisting it. Does not require a live API call.
     */
    boolean validateMemberToken(String externalMemberToken);
}
```

Provider implementations are CDI beans and discovered automatically. The `LoyaltyProviderRegistry` resolves the correct provider at runtime based on `user.region` and the presence of a linked membership.

---

## Data Model

Two new tables are introduced in the `cerebralstratum` schema (Liquibase-managed):

### `loyalty_memberships`

Stores the link between a platform user and their external loyalty programme membership.

| Column                  | Type           | Notes                                |
|-------------------------|----------------|--------------------------------------|
| `id`                    | `uuid`         | PK                                   |
| `user_id`               | `uuid`         | FK → `users.id`, ON DELETE CASCADE   |
| `provider_id`           | `varchar(64)`  | e.g. `flybuys`, `airpoints`          |
| `external_member_token` | `varchar(255)` | Loyalty programme member number/token |
| `linked_at`             | `timestamp`    | When the user linked the account     |
| `active`                | `boolean`      | User can deactivate without deleting |

Unique constraint: `(user_id, provider_id)` — one membership per provider per user.

### `loyalty_events`

Append-only audit log of all points award attempts. Enables idempotency enforcement and dispute resolution.

| Column               | Type           | Notes                                       |
|----------------------|----------------|---------------------------------------------|
| `id`                 | `uuid`         | PK                                          |
| `user_id`            | `uuid`         | FK → `users.id`                             |
| `provider_id`        | `varchar(64)`  | Discriminator                               |
| `shopify_order_id`   | `varchar(255)` | Source-of-truth event identifier            |
| `points_awarded`     | `integer`      | Points awarded (0 if failed)                |
| `status`             | `varchar(32)`  | `PENDING`, `AWARDED`, `FAILED`, `SKIPPED`   |
| `attempted_at`       | `timestamp`    |                                             |
| `provider_reference` | `varchar(255)` | Provider's confirmation reference, nullable |
| `failure_reason`     | `text`         | Populated on `FAILED` status                |

Unique constraint: `(user_id, shopify_order_id, provider_id)` — enforces idempotency at the database level.

---

## Renewal Flow

Points award is a **side effect** of renewal, not part of the critical path. The Shopify `subscription_contract_renewed` webhook is the trigger.

```
Shopify webhook: subscription_contract_renewed
        │
        ▼
BillingWebhookResource (Quarkus REST)
  - Validates Shopify HMAC signature
  - Publishes SubscriptionRenewalEvent to internal CDI event bus
  - Returns 200 immediately
        │
        ▼ (async, @Observes @Asynchronous)
EntitlementSyncService
  - Updates subscription state in PostgreSQL (existing behaviour)
        │
        ▼ (async side-effect, same event)
LoyaltyAwardService
  - Resolves user region
  - Checks LOYALTY_INTEGRATION_ENABLED flag
  - Checks user has an active loyalty_membership for their region
  - Checks loyalty_events for existing record (idempotency guard)
  - Writes PENDING record to loyalty_events (outbox)
  - Calls LoyaltyProvider.awardRenewalPoints(...)
  - Updates loyalty_events record to AWARDED or FAILED
```

Key invariants:

- Webhook handler always returns `200` regardless of loyalty award outcome.
- A failure in `LoyaltyAwardService` must never propagate to `EntitlementSyncService`.
- `FAILED` events are retryable via a Quarkus `@Scheduled` task (configurable retry window, defaulting to 3 attempts with exponential backoff).
- `SKIPPED` is written when the user has no linked membership or is not in a supported region — this is not an error condition.

---

## Region Gating

The `Region` enum maps to ISO 3166-1 alpha-2 country codes derived from the Shopify billing address at subscription creation time:

```java
public enum Region {
    AU, NZ, US, GB, DE, FR  // extend as partnerships are established
}
```

Provider-to-region mapping at launch (subject to commercial outcomes):

| Region | Programme              | Status                            |
|--------|------------------------|-----------------------------------|
| `AU`   | Flybuys                | Pending commercial agreement      |
| `NZ`   | Flybuys NZ / Airpoints | Under evaluation                  |
| `US`   | TBD                    | No dominant equivalent identified |
| `EU`   | TBD                    | Fragmented; deferred              |

---

## Alternatives Considered

### 1. Direct Shopify integration via Shopify Flow + external app

Shopify Flow could trigger a webhook on renewal. However, this couples the loyalty logic to Shopify's workflow engine rather than the platform's own service layer, making it harder to enforce idempotency, add retry logic, or support multiple regions. Rejected in favour of server-side ownership.

### 2. Client-side loyalty number entry only, no server-side API call

Some programmes accept points claims via batch file upload rather than real-time API. This would simplify the integration but introduce significant delay in point crediting and create a reconciliation burden. Deferred as a fallback if the target provider does not offer a real-time earn API.

### 3. Build a proprietary BlueGuardian points system

Could offer points redeemable against subscription discounts or hardware purchases. Provides full control but requires significant product effort and has no established member base. Rejected for initial phase; may be revisited as a complement to external programmes.

### 4. Single hardcoded Flybuys integration

Simpler to implement but creates a rework obligation when additional regions are onboarded. The provider abstraction has minimal overhead and eliminates future design-time risk. Rejected.

---

## Consequences

**Positive:**

- Loyalty integration does not block or complicate the subscription renewal critical path.
- Pluggable design means additional regional providers can be added without modifying existing code.
- `loyalty_events` table provides a complete audit trail for member dispute resolution and internal reconciliation.
- Idempotency at the database level (`UNIQUE` constraint on `(user_id, shopify_order_id, provider_id)`) protects against Shopify webhook redelivery.
- Feature flag means the integration can be shipped in code before any commercial partnership exists, and toggled on per-environment when ready.

**Negative / Trade-offs:**

- A commercial agreement with Loyalty Pacific (Flybuys) is a prerequisite for AU market delivery. Architecture work can proceed but the feature cannot go live until BD concludes.
- The actual Flybuys earn API shape, authentication mechanism, and points rate are all subject to negotiation — the `LoyaltyProvider` interface may require adjustment once the API is known.
- Adding a `loyalty_memberships` entry to the user profile flow adds UX surface area. Needs a dedicated account-linking screen in the KMP client.
- Users in unsupported regions (US, EU at launch) will not benefit from this feature, which may create perceived inequity.

---

## Open Items

- [ ] Initiate commercial partnership discussion with Loyalty Pacific (Flybuys) — BD action, not engineering.
- [ ] Evaluate Airpoints (Air New Zealand) as an alternative/complement for the NZ market.
- [ ] Determine points earn rate per renewal (likely expressed as points per AUD spent, subject to negotiation).
- [ ] Confirm Flybuys earn API authentication model (OAuth2, API key, or client certificate) — `LoyaltyProvider` interface may need adjustment.
- [ ] Design the account-linking UX in KMP — the `loyalty_memberships` entry should be user-initiated from an account settings screen, with explicit privacy notice.
- [ ] Assess privacy implications of storing `external_member_token` under AU Privacy Act and GDPR. Consider whether it should be stored encrypted at rest (likely yes, via PostgreSQL `pgcrypto` or application-layer encryption).
- [ ] Define the retry schedule and alerting threshold for `FAILED` loyalty events.
- [ ] Decide whether to surface loyalty event history to users in the KMP client.

---

## Forward Pointers

- Account-linking UX will need to be considered alongside the KMP client account settings design.
- If a proprietary BlueGuardian points system is revisited in future, it should extend rather than replace this abstraction — the `LoyaltyProvider` interface can accommodate a first-party provider implementation.
