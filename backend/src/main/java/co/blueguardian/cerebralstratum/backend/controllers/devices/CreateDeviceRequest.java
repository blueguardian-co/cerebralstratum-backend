package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.UUID;

public class CreateDeviceRequest {

    public UUID uuid;

    public CreateDeviceRequest() {
    }

    public CreateDeviceRequest(
            UUID uuid
    ) {
        this.uuid = uuid;
    }
}
