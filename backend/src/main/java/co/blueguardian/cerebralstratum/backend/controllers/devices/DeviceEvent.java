package co.blueguardian.cerebralstratum.backend.controllers.devices;

import java.util.UUID;

public sealed interface DeviceEvent permits DeviceLocationEvent, DeviceStatusEvent, DeviceCanBusEvent {
    UUID deviceId();
}
