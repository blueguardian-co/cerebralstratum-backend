package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.util.UUID;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Single point of Kafka consumption for device telemetry, fanned out in-process to any
 * number of subscribers (gRPC, SSE, per-device or otherwise) via BroadcastProcessor.
 * One Kafka consumer per topic, regardless of how many transports/devices are watching.
 */
@ApplicationScoped
public class DeviceEventBroadcaster {

    private final BroadcastProcessor<DeviceLocationEvent> locationEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceStatusEvent> statusEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceCanBusEvent> canBusEvents = BroadcastProcessor.create();

    @Incoming("device.location")
    void onLocation(ConsumerRecord<UUID, GetLocationRequest> record) {
        locationEvents.onNext(new DeviceLocationEvent(record.key(), record.value()));
    }

    @Incoming("device.status")
    void onStatus(ConsumerRecord<UUID, Status> record) {
        statusEvents.onNext(new DeviceStatusEvent(record.key(), record.value()));
    }

    @Incoming("device.canbus")
    void onCanBus(ConsumerRecord<UUID, CANBus> record) {
        canBusEvents.onNext(new DeviceCanBusEvent(record.key(), record.value()));
    }

    public Multi<DeviceLocationEvent> locationUpdatesFor(UUID deviceId) {
        return locationEvents.filter(event -> event.deviceId().equals(deviceId));
    }

    public Multi<DeviceStatusEvent> statusUpdatesFor(UUID deviceId) {
        return statusEvents.filter(event -> event.deviceId().equals(deviceId));
    }

    public Multi<DeviceCanBusEvent> canBusUpdatesFor(UUID deviceId) {
        return canBusEvents.filter(event -> event.deviceId().equals(deviceId));
    }
}
