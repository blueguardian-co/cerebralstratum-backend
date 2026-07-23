package co.blueguardian.cerebralstratum.backend.controllers.locations;

import co.blueguardian.cerebralstratum.utils.model.PointDeserializer;
import co.blueguardian.cerebralstratum.utils.model.PointSerializer;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.locationtech.jts.geom.Point;

public class GetLocationRequest {

    // quarkus-rest-jackson generates build-time serializers for REST response types and
    // can't see the runtime-registered GeometryJacksonModule, so it falls back to reflecting
    // over Point's self-referential getters (getEnvelope(), getCentroid(), ...) and recurses
    // forever. Pointing it at the real serializer/deserializer directly avoids that.
    @JsonSerialize(using = PointSerializer.class)
    @JsonDeserialize(using = PointDeserializer.class)
    public Point coordinates; // x,y,z (need to ensure we include height)
    public int update_frequency;
    public int accuracy;
    public double speed;
    public double bearing;
    public LocalDateTime timestamp;

    public GetLocationRequest() {
    }

    public GetLocationRequest(
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

    @Override
    public String toString() {
        return "-- Status --" +
                "Coordinates: " + coordinates + '\n' +
                "Update Frequency: " + update_frequency + '\n' +
                "Accuracy: " + accuracy + '\n' +
                "Speed: " + speed + '\n' +
                "Bearing: " + bearing + '\n' +
                "Timestamp: " + timestamp;
    }
}
