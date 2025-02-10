package co.blueguardian.cerebralstratum.utils.messaging;

import java.util.UUID;

public class StatusMessage {
    public UUID device_id;
    public String summary;
    public String overall;
    public float battery;

    public StatusMessage() {
    }

    public StatusMessage(
            UUID device_id,
            String summary,
            String overall,
            float battery
    ){
        this.device_id = device_id;
        this.summary = summary;
        this.overall = overall;
        this.battery = battery;
    }
}
