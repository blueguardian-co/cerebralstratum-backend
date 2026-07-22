package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;

import java.util.UUID;

public record DeviceLocationEvent(UUID deviceId, GetLocationRequest location) implements DeviceEvent {
}
