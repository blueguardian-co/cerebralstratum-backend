package co.blueguardian.cerebralstratum.backend.controllers.locations;

import java.time.LocalDateTime;
import org.locationtech.jts.geom.Point;

public class GetLocationRequest {

    public Point coordinates;
    public LocalDateTime timestamp;

    public GetLocationRequest() {
    }

    public GetLocationRequest(
            Point coordinates,
            LocalDateTime timestamp
    ) {
        this.coordinates = coordinates;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "-- Status --" +
                "Coordinates: " + coordinates + '\n' +
                "Timestamp: " + timestamp;
    }
}
