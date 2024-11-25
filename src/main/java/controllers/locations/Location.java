package controllers.locations;

import java.time.LocalDateTime;

public class Location {

    public int id;
    public int device_id;
    public LocalDateTime timestamp;

    public Location() {
    }

    public Location(
            int id,
            int device_id,
            LocalDateTime timestamp
    ) {
        this.id = id;
        this.device_id = device_id;
        this.timestamp = timestamp;
    }
}
