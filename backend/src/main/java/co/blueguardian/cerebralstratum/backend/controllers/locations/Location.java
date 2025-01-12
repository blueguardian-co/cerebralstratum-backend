package co.blueguardian.cerebralstratum.backend.controllers.locations;

import java.time.LocalDateTime;
import java.util.UUID;

public class Location {

    public int id;
    public UUID device_id;
    public LocalDateTime timestamp;

    public Location() {
    }

    public Location(
            int id,
            UUID device_id,
            LocalDateTime timestamp
    ) {
        this.id = id;
        this.device_id = device_id;
        this.timestamp = timestamp;
    }
}
