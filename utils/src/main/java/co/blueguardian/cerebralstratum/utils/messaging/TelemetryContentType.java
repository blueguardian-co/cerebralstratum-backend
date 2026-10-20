package co.blueguardian.cerebralstratum.utils.messaging;

/**
 * Content-type values used to discriminate telemetry kind on the single
 * per-tenant Kafka topic (hono.telemetry.&lt;tenant&gt;) that Eclipse Hono's Kafka-based
 * protocol adapters publish all telemetry types to — see the Hono Kafka Telemetry
 * API spec (https://eclipse.dev/hono/docs/api/telemetry-kafka/). Devices set this via
 * their MQTT/CoAP/HTTP publish request; Hono forwards it as the Kafka record's
 * content-type header.
 */
public final class TelemetryContentType {

    public static final String LOCATION = "application/vnd.cerebral-stratum.telemetry.location+json";
    public static final String STATUS = "application/vnd.cerebral-stratum.telemetry.status+json";
    public static final String CANBUS = "application/vnd.cerebral-stratum.telemetry.canbus+json";

    private TelemetryContentType() {
    }
}
