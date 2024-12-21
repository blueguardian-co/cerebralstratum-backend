package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.UUID;

public class RegisterDeviceRequest {

    public UUID serial_number;

    public RegisterDeviceRequest() {
    }

    public RegisterDeviceRequest(
            UUID serial_number
    ) {
        this.serial_number = serial_number;
    }
}
