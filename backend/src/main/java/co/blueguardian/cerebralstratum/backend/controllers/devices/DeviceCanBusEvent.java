package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.UUID;

public record DeviceCanBusEvent(UUID deviceId, CANBus canBus) implements DeviceEvent {
}
