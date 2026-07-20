# ADR-0008: Privacy Zones

| Field | Value |
|---|---|
| **Status** | Proposed |
| **Date** | 2026-07-20 |
| **Author** | Alex Henshaw |
| **Relates to** | Core principle: PostGIS-authoritative geofencing |

---

## Context

Device owners may want certain locations — home, a partner's workplace, a place of worship, a medical clinic — excluded from the precise location trail visible to anyone with device access, including the owner themselves in shared-viewing contexts (e.g. a family plan where one member can see another's device). A Privacy Zone lets an owner define a geographic area within which precise location is suppressed, while still allowing the rest of the trail (arrival direction, general activity) to be meaningful.

The naive implementation — simply omitting location points while inside the zone — leaks the zone's boundary anyway: an observer can infer the zone's edge almost exactly from where the trail disappears (entry) and where it reappears (exit). If suppression and resumption both happen at the literal zone boundary, the zone is trivially reverse-engineered from two clean cut points, which defeats the purpose of having a "privacy" zone at all.

## Decision

**Symmetric, independently-drawn jitter at both entry and exit.** Both the point at which location suppression begins (on approach/entry) and the point at which location resumption begins (on exit) must be:
- **Jittered** — offset from the true zone boundary by a randomized distance/bearing, not suppressed/resumed exactly at the boundary.
- **Independently drawn** — the entry jitter and exit jitter are separate random draws, not mirrored or derived from each other. If exit jitter were simply the entry jitter reflected, an observer could still infer the boundary by averaging the two suppression/resumption points.
- **Ephemeral** — each traversal of the zone draws fresh jitter values; the same zone is not jittered identically every time it's entered or exited, which would otherwise let repeated observation converge on the true boundary through averaging across multiple trips.

**Enforcement point:** Privacy Zone evaluation happens server-side, consistent with the platform-wide principle that geofencing (via PostGIS) is server-side and authoritative — the client never independently decides when to suppress a point, and jitter values are computed and applied server-side before location data is persisted or served to any viewer, including the owner's own dashboard views to other shared parties.

**Scope of suppression:** While inside a zone (accounting for jitter), no precise coordinate is recorded or served for shared/non-owner views. The owner's own primary account view may retain full-fidelity data (open item — see below) since the owner already knows their own location; the privacy concern is about a *shared viewer* inferring the zone, not the owner losing their own data.

## Alternatives Considered

- **Suppress exactly at the literal zone boundary, no jitter.** Rejected — trivially leaks the boundary from the exact suppression/resumption points, defeating the purpose.
- **Mirrored jitter (exit jitter = negated entry jitter).** Rejected — still allows boundary inference by averaging entry and exit points across a single traversal, since the two are mathematically related.
- **Fixed jitter distance reused across all traversals of the same zone.** Rejected — repeated observation of the same zone across multiple trips would let an observer average out the fixed offset and converge on the true boundary; jitter must be redrawn per traversal.
- **Suppress the entire trip (origin to destination) whenever a Privacy Zone is involved anywhere in the trip.** Rejected as the default — too aggressive a suppression for the stated use case (protecting one specific location, not the whole journey); may be revisited as an optional stricter mode if requested, but is not the baseline behavior.

## Consequences

- Meaningfully closes the boundary-inference gap that naive suppression leaves open, at the cost of some fuzziness in exactly where a trail "restarts" after a zone — an acceptable tradeoff since the entire point is to obscure the precise boundary.
- Jitter must be generated and applied server-side per-traversal, adding a small amount of computation to the location-ingestion path whenever a device is near a configured Privacy Zone — not expected to be significant at current or near-term device-fleet scale.
- Testing this correctly requires statistical/property-based tests (confirming jitter is independently drawn and doesn't converge under repeated simulated traversals) rather than simple point-in-polygon unit tests — a different testing approach than most other PostGIS-backed features.

## Open Items

- Does the account owner's own primary dashboard view see jitter-free data inside their own Privacy Zones, or is jitter applied universally including to the owner's own view? Leaning toward owner sees full fidelity, shared viewers see jittered/suppressed data — needs explicit confirmation before implementation.
- Jitter magnitude bounds (min/max offset distance) — needs a concrete number, likely configurable per zone or per tenant tier, to be defined during implementation.
- Whether Privacy Zones interact with Anomaly Detection — does the anomaly model train on jittered or true coordinates? Leaning toward the model needs true coordinates (it's a first-party, non-shared system) with jitter applied only at the serving/sharing layer, but this needs explicit confirmation since Anomaly Detection is per-tenant/per-device and its own data-handling boundary isn't yet specified relative to Privacy Zones.
- Interaction with the Emergency SOS ADR — does an SOS event override Privacy Zone suppression (i.e. show true location to emergency contacts even inside a zone)? This seems like an obvious "yes" on safety grounds but needs to be an explicit, named exception rather than an assumed one, mirroring how the entitlement bypass was handled explicitly in the SOS ADR.
