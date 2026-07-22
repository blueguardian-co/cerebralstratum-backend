package co.blueguardian.cerebralstratum.devicesimulator.kafka;

/**
 * Mirrors the field shape of backend's
 * {@code co.blueguardian.cerebralstratum.backend.controllers.devices.CANBus} —
 * the {@code device.canbus} Kafka value type. See {@link LocationEvent} for why this
 * is a structural mirror rather than a shared type.
 */
public class CanBusEvent {
    public String payload;

    public CanBusEvent() {
    }

    public CanBusEvent(String payload) {
        this.payload = payload;
    }
}
