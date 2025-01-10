package co.blueguardian.cerebralstratum.backend.controllers.locations;

import java.time.LocalDateTime;

/*
TODO:
- Use the GeoTools library to define the Latitude and Longitude types
 */

public class InboundLocation {

    public LocalDateTime timestamp;

    public InboundLocation() {
    }

    public InboundLocation(
            LocalDateTime timestamp
    ) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "-- Status --" +
                "Timestamp: " + timestamp;
    }
}
