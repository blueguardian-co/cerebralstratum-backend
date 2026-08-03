package co.blueguardian.cerebralstratum.devicesimulator;

import java.util.UUID;

import co.blueguardian.cerebralstratum.devicesimulator.kafka.CanBusEvent;
import co.blueguardian.cerebralstratum.devicesimulator.kafka.LocationEvent;
import co.blueguardian.cerebralstratum.utils.messaging.CANBusMessage;
import co.blueguardian.cerebralstratum.utils.messaging.LocationMessage;
import co.blueguardian.cerebralstratum.utils.messaging.StatusMessage;
import co.blueguardian.cerebralstratum.utils.model.Status;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

/**
 * Dev-only bridge from MQTT (published by this same simulator) onto the Kafka topics
 * backend's DeviceStreamService consumes. Stands in for the real edge middleware
 * (Eclipse Hono, per the ADRs) so the gRPC/DB path can be exercised locally without it.
 */
@ApplicationScoped
class DeviceMessageBridge {

    private static final Logger LOG = Logger.getLogger(DeviceMessageBridge.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Channel("kafka-location")
    Emitter<LocationEvent> locationEmitter;

    @Inject
    @Channel("kafka-status")
    Emitter<Status> statusEmitter;

    @Inject
    @Channel("kafka-canbus")
    Emitter<CanBusEvent> canbusEmitter;

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
            locationEmitter.send(keyed(event, message.device_id));
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
            statusEmitter.send(keyed(status, message.device_id));
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
            canbusEmitter.send(keyed(event, message.device_id));
        } catch (Exception e) {
            LOG.errorf(e, "Discarding malformed canbus payload: %s", payload);
        }
    }

    private static <T> Message<T> keyed(T payload, UUID deviceId) {
        return Message.of(payload)
                .addMetadata(OutgoingKafkaRecordMetadata.<UUID>builder().withKey(deviceId).build());
    }
}
