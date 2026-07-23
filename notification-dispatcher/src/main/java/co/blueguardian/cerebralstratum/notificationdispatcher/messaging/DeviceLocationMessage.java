package co.blueguardian.cerebralstratum.notificationdispatcher.messaging;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Point;

/**
 * Mirrors the field shape of backend's
 * {@code co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest} —
 * the {@code device.location} Kafka value type. Kafka JSON (de)serialization is structural
 * (Jackson field-name matching), not tied to Java class identity, so this intentionally
 * duplicates that shape rather than depending on the backend module — the same convention
 * device-simulator's LocationEvent already uses for the producer side.
 */
public class DeviceLocationMessage {
    public Point coordinates;
    public int update_frequency;
    public int accuracy;
    public double speed;
    public double bearing;
    public LocalDateTime timestamp;

    public DeviceLocationMessage() {
    }
}