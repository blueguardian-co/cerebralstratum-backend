package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.utils.model.Status;

import java.util.UUID;

public record DeviceStatusEvent(UUID deviceId, Status status) implements DeviceEvent {
}
