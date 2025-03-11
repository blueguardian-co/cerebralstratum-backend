package co.blueguardian.cerebralstratum.utils.messaging;

import java.util.UUID;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import org.locationtech.jts.geom.Point;

@JsonDeserialize(using = LocationMessageDeserializer.class)
public class LocationMessage {
    public UUID device_id;
    public Point coordinates; // x,y (need to add height)
    public int update_frequency;
    public int accuracy;
    public double speed;
    public double bearing;
    public LocalDateTime timestamp;

    public LocationMessage() {
    }

    public LocationMessage(
            UUID device_id,
            Point coordinates,
            int update_frequency,
            int accuracy,
            double speed,
            double bearing,
            LocalDateTime timestamp
    ){
        this.device_id = device_id;
        this.coordinates = coordinates;
        this.update_frequency = update_frequency;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = timestamp;
    }
}
