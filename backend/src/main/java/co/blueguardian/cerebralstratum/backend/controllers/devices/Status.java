package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.time.LocalDateTime;

public class Status {
    public String summary;
    public String overall;
    public float battery;
    public LocalDateTime timestamp;

    public Status(){
    }

    public Status(
            String summary,
            String overall,
            float battery,
            LocalDateTime timestamp
    ){
        this.summary = summary;
        this.overall = overall;
        this.battery = battery;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "-- Status --" +
               "Summary: " + summary + '\n' +
               "Overall: " + overall + '\n' +
               "Battery: " + battery + "\n" +
               "Timestamp: " + timestamp;
    }
}
