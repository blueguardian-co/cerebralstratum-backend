package co.blueguardian.cerebralstratum.notificationdispatcher.messaging;

import co.blueguardian.cerebralstratum.notificationdispatcher.notifications.NotificationDispatchService;
import co.blueguardian.cerebralstratum.utils.messaging.TelemetryContentType;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

/**
 * Consumes the same single hono.telemetry.&lt;tenant&gt; topic backend's
 * DeviceEventBroadcaster reads (see ADR-0010) — there is no dedicated "notification" topic
 * yet, so device telemetry is the source signal for this bootstrap consumer. Hono's Kafka
 * protocol carries all telemetry types on one topic, demuxed by content-type header (see
 * the Hono Kafka Telemetry API spec), so this listens for location/status only and ignores
 * canbus. Deciding which telemetry actually warrants a push notification is future scope;
 * this consumer only proves the Kafka -> Redis dedupe -> Postgres pipeline end to end.
 */
@ApplicationScoped
public class NotificationEventConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationEventConsumer.class);

    private final NotificationDispatchService dispatchService;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(NotificationDispatchService dispatchService, ObjectMapper objectMapper) {
        this.dispatchService = dispatchService;
        this.objectMapper = objectMapper;
    }

    @Incoming("device-telemetry")
    void onTelemetry(ConsumerRecord<String, byte[]> record) {
        UUID deviceId = UUID.fromString(record.key());
        String contentType = headerValue(record, "content-type");
        try {
            switch (contentType == null ? "" : contentType) {
                case TelemetryContentType.LOCATION -> {
                    DeviceLocationMessage location = objectMapper.readValue(record.value(), DeviceLocationMessage.class);
                    dispatchService.dispatch(deviceId, "LOCATION", location.timestamp);
                }
                case TelemetryContentType.STATUS -> {
                    Status status = objectMapper.readValue(record.value(), Status.class);
                    dispatchService.dispatch(deviceId, "STATUS", status.timestamp);
                }
                case TelemetryContentType.CANBUS -> {
                    // Not a notification signal (yet) — ignored.
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
}