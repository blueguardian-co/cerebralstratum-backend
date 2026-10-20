package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.backend.repositories.locations.LocationRepository;
import co.blueguardian.cerebralstratum.backend.repositories.statuses.StatusRepository;
import co.blueguardian.cerebralstratum.utils.messaging.TelemetryContentType;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Single point of Kafka consumption for device telemetry, fanned out in-process to any
 * number of subscribers (gRPC, SSE, per-device or otherwise) via BroadcastProcessor.
 * One Kafka consumer per topic, regardless of how many transports/devices are watching.
 *
 * <p>Consumes Eclipse Hono's single per-tenant Kafka topic (hono.telemetry.&lt;tenant&gt;),
 * which carries all telemetry types together — Hono's Kafka protocol doesn't support
 * per-type topics, so the telemetry kind is demuxed from the record's content-type
 * header (see the Hono Kafka Telemetry API spec) rather than the topic name.
 */
@ApplicationScoped
public class DeviceEventBroadcaster {

    private static final Logger LOG = Logger.getLogger(DeviceEventBroadcaster.class);

    @Inject
    StatusRepository statusRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    ObjectMapper objectMapper;

    private final BroadcastProcessor<DeviceLocationEvent> locationEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceStatusEvent> statusEvents = BroadcastProcessor.create();
    private final BroadcastProcessor<DeviceCanBusEvent> canBusEvents = BroadcastProcessor.create();

    @Incoming("device-telemetry")
    void onTelemetry(ConsumerRecord<String, byte[]> record) {
        UUID deviceId = UUID.fromString(record.key());
        String contentType = headerValue(record, "content-type");
        try {
            switch (contentType == null ? "" : contentType) {
                case TelemetryContentType.LOCATION -> {
                    GetLocationRequest location = objectMapper.readValue(record.value(), GetLocationRequest.class);
                    locationEvents.onNext(new DeviceLocationEvent(deviceId, location));
                    locationRepository.record(deviceId, location);
                }
                case TelemetryContentType.STATUS -> {
                    Status status = objectMapper.readValue(record.value(), Status.class);
                    statusEvents.onNext(new DeviceStatusEvent(deviceId, status));
                    statusRepository.record(deviceId, status);
                }
                case TelemetryContentType.CANBUS -> {
                    CANBus canBus = objectMapper.readValue(record.value(), CANBus.class);
                    canBusEvents.onNext(new DeviceCanBusEvent(deviceId, canBus));
                }
                default -> LOG.warnf("Discarding telemetry with unrecognized content-type '%s' from device %s",
                        contentType, deviceId);
            }
        } catch (Exception e) {
            LOG.errorf(e, "Discarding malformed telemetry payload from device %s (content-type=%s)",
                    deviceId, contentType);
        }
    }

    private static String headerValue(ConsumerRecord<?, ?> record, String key) {
        Header header = record.headers().lastHeader(key);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
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
