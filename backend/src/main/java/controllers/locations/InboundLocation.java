package controllers.locations;

import java.time.LocalDateTime;

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
