package co.blueguardian.cerebralstratum.utils.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class DeviceStatus {

    public int id;
    public UUID device_id;
    public String summary;
    public String overall;
    public float battery;
    public LocalDateTime timestamp;
    public LocalDateTime expires_at;

    public DeviceStatus() {
    }

    public DeviceStatus(
            int id,
            UUID device_id,
            String summary,
            String overall,
            float battery,
            LocalDateTime timestamp,
            LocalDateTime expires_at
    ) {
        this.id = id;
        this.device_id = device_id;
        this.summary = summary;
        this.overall = overall;
        this.battery = battery;
        this.timestamp = timestamp;
        this.expires_at = expires_at;
    }
}