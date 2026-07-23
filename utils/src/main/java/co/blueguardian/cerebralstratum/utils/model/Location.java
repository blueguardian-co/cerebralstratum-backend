package co.blueguardian.cerebralstratum.utils.model;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Point;

public class Location {

    public int id;
    public UUID device_id;
    // quarkus-rest-jackson generates build-time serializers for REST response types and
    // can't see the runtime-registered GeometryJacksonModule, so it falls back to reflecting
    // over Point's self-referential getters (getEnvelope(), getCentroid(), ...) and recurses
    // forever. Pointing it at the real serializer/deserializer directly avoids that.
    @JsonSerialize(using = PointSerializer.class)
    @JsonDeserialize(using = PointDeserializer.class)
    public Point coordinates;
    public int update_frequency;
    public int accuracy;
    public double speed;
    public double bearing;
    public LocalDateTime timestamp;

    public Location() {
    }

    public Location(
            int id,
            UUID device_id,
            Point coordinates,
            int update_frequency,
            int accuracy,
            double speed,
            double bearing,
            LocalDateTime timestamp
    ) {
        this.id = id;
        this.device_id = device_id;
        this.coordinates = coordinates;
        this.update_frequency = update_frequency;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = timestamp;
    }
}
