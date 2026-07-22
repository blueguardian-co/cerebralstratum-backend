package co.blueguardian.cerebralstratum.devicesimulator.kafka;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Point;

/**
 * Mirrors the field shape of backend's
 * {@code co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest} —
 * the {@code device.location} Kafka value type. Kafka JSON (de)serialization is
 * structural (Jackson field-name matching), not tied to Java class identity, so this
 * intentionally duplicates that shape rather than depending on the backend module.
 * This dev-only bridge exists because the real MQTT-to-Kafka middleware described in
 * the ADRs doesn't exist yet — device-registrar owns device lifecycle (onboarding,
 * association), not message bridging.
 */
public class LocationEvent {
    public Point coordinates;
    public int update_frequency;
    public int accuracy;
    public double speed;
    public double bearing;
    public LocalDateTime timestamp;

    public LocationEvent() {
    }

    public LocationEvent(
            Point coordinates,
            int update_frequency,
            int accuracy,
            double speed,
            double bearing,
            LocalDateTime timestamp
    ) {
        this.coordinates = coordinates;
        this.update_frequency = update_frequency;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = timestamp;
    }
}
