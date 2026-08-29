package co.blueguardian.cerebralstratum.devicesimulator;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import co.blueguardian.cerebralstratum.devicesimulator.kafka.CanBusEvent;
import co.blueguardian.cerebralstratum.devicesimulator.kafka.LocationEvent;
import co.blueguardian.cerebralstratum.utils.messaging.CANBusMessage;
import co.blueguardian.cerebralstratum.utils.messaging.LocationMessage;
import co.blueguardian.cerebralstratum.utils.messaging.StatusMessage;
import co.blueguardian.cerebralstratum.utils.messaging.TelemetryContentType;
import co.blueguardian.cerebralstratum.utils.model.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Dev-only bridge from MQTT (published by this same simulator) onto the single
 * hono.telemetry.&lt;tenant&gt; Kafka topic backend's DeviceEventBroadcaster consumes,
 * demuxed by content-type header — the same shape Eclipse Hono's Kafka-based protocol
 * adapters produce (see the Hono Kafka Telemetry API spec). Stands in for the real edge
 * middleware (Eclipse Hono, per the ADRs) so the gRPC/DB path can be exercised locally
 * without it.
 */
@ApplicationScoped
class DeviceMessageBridge {

    private static final Logger LOG = Logger.getLogger(DeviceMessageBridge.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Channel("device-telemetry")
    Emitter<byte[]> telemetryEmitter;

    @Incoming("mqtt-location")
    void consumeLocation(String payload) {
        LOG.debugf("Received location payload from MQTT: %s", payload);
        try {
            LocationMessage message = objectMapper.readValue(payload, LocationMessage.class);
            LocationEvent event = new LocationEvent(
                    message.coordinates,
                    message.update_frequency,
                    message.accuracy,
                    message.speed,
                    message.bearing,
                    message.timestamp
            );
            publish(message.device_id, TelemetryContentType.LOCATION, event);
        } catch (Exception e) {
            LOG.errorf(e, "Discarding malformed location payload: %s", payload);
        }
    }

    @Incoming("mqtt-status")
    void consumeStatus(String payload) {
        LOG.debugf("Received status payload from MQTT: %s", payload);
        try {
            StatusMessage message = objectMapper.readValue(payload, StatusMessage.class);
            Status status = new Status(message.summary, message.overall, message.battery, message.timestamp);
            publish(message.device_id, TelemetryContentType.STATUS, status);
        } catch (Exception e) {
            LOG.errorf(e, "Discarding malformed status payload: %s", payload);
        }
    }

    @Incoming("mqtt-canbus")
    void consumeCanBus(String payload) {
        LOG.debugf("Received canbus payload from MQTT: %s", payload);
        try {
            CANBusMessage message = objectMapper.readValue(payload, CANBusMessage.class);
            CanBusEvent event = new CanBusEvent(message.payload);
            publish(message.device_id, TelemetryContentType.CANBUS, event);
        } catch (Exception e) {
            LOG.errorf(e, "Discarding malformed canbus payload: %s", payload);
        }
    }

    private void publish(UUID deviceId, String contentType, Object payload) throws Exception {
        byte[] value = objectMapper.writeValueAsBytes(payload);
        telemetryEmitter.send(Message.of(value)
                .addMetadata(OutgoingKafkaRecordMetadata.<String>builder()
                        .withKey(deviceId.toString())
                        .withHeaders(new RecordHeaders()
                                .add("content-type", contentType.getBytes(StandardCharsets.UTF_8)))
                        .build()));
    }
}
