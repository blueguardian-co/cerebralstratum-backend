package co.blueguardian.cerebralstratum.utils.messaging;

import java.util.UUID;

public class CANBusMessage {
    public UUID device_id;
    public String payload;

    public CANBusMessage() {
    }

    public CANBusMessage(
            UUID device_id,
            String payload
    ) {
        this.device_id = device_id;
        this.payload = payload;
    }
}
