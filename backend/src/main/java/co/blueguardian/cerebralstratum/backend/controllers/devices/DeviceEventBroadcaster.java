package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.backend.repositories.locations.LocationRepository;
import co.blueguardian.cerebralstratum.backend.repositories.statuses.StatusRepository;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.util.Set;
import java.util.UUID;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Single point of Kafka consumption for device telemetry, fanned out in-process to any
 * number of subscribers (gRPC, SSE, per-device or otherwise) via BroadcastProcessor.
 * One Kafka consumer per topic, regardless of how many transports/devices are watching.
 */
@ApplicationScoped
public class DeviceEventBroadcaster {

    @Inject
    StatusRepository statusRepository;

    @Inject
    LocationRepository locationRepository;

    private final BroadcastProcessor<DeviceLocationEvent> locationEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceStatusEvent> statusEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceCanBusEvent> canBusEvents = BroadcastProcessor.create();

    @Incoming("device.location")
    void onLocation(ConsumerRecord<UUID, GetLocationRequest> record) {
        locationEvents.onNext(new DeviceLocationEvent(record.key(), record.value()));
        locationRepository.record(record.key(), record.value());
    }

    @Incoming("device.status")
    void onStatus(ConsumerRecord<UUID, Status> record) {
        statusEvents.onNext(new DeviceStatusEvent(record.key(), record.value()));
        statusRepository.record(record.key(), record.value());
    }

    @Incoming("device.canbus")
    void onCanBus(ConsumerRecord<UUID, CANBus> record) {
        canBusEvents.onNext(new DeviceCanBusEvent(record.key(), record.value()));
    }

    public Multi<DeviceLocationEvent> locationUpdatesFor(UUID deviceId) {
        return locationUpdatesFor(Set.of(deviceId));
    }

    public Multi<DeviceStatusEvent> statusUpdatesFor(UUID deviceId) {
        return statusUpdatesFor(Set.of(deviceId));
    }

    public Multi<DeviceCanBusEvent> canBusUpdatesFor(UUID deviceId) {
        return canBusUpdatesFor(Set.of(deviceId));
    }

    /**
     * Multiplexed variants backing the multi-device SSE endpoints (DeviceMultiplexedServerSentEvents):
     * one BroadcastProcessor subscription filtered against a caller-specific device set, rather
     * than one subscription per device. {@code deviceIds} must already be access-checked — this
     * is an in-process filter, not an authorization boundary.
     */
    public Multi<DeviceLocationEvent> locationUpdatesFor(Set<UUID> deviceIds) {
        return locationEvents.filter(event -> deviceIds.contains(event.deviceId()));
    }

    public Multi<DeviceStatusEvent> statusUpdatesFor(Set<UUID> deviceIds) {
        return statusEvents.filter(event -> deviceIds.contains(event.deviceId()));
    }

    public Multi<DeviceCanBusEvent> canBusUpdatesFor(Set<UUID> deviceIds) {
        return canBusEvents.filter(event -> deviceIds.contains(event.deviceId()));
    }
}
