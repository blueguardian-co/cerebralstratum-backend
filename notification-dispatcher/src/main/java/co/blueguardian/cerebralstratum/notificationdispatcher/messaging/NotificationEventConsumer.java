package co.blueguardian.cerebralstratum.notificationdispatcher.messaging;

import co.blueguardian.cerebralstratum.notificationdispatcher.notifications.NotificationDispatchService;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

/**
 * Consumes the same {@code device.location}/{@code device.status} topics backend's
 * DeviceEventBroadcaster reads (see ADR-0010) — there is no dedicated "notification" topic
 * yet, so these device-telemetry topics are the source signal for this bootstrap consumer.
 * Deciding which telemetry actually warrants a push notification is future scope; this
 * consumer only proves the Kafka -> Redis dedupe -> Postgres pipeline end to end.
 */
@ApplicationScoped
public class NotificationEventConsumer {

    private final NotificationDispatchService dispatchService;

    public NotificationEventConsumer(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @Incoming("device.location")
    void onLocation(ConsumerRecord<UUID, DeviceLocationMessage> record) {
        dispatchService.dispatch(record.key(), "LOCATION", record.value().timestamp);
    }

    @Incoming("device.status")
    void onStatus(ConsumerRecord<UUID, Status> record) {
        dispatchService.dispatch(record.key(), "STATUS", record.value().timestamp);
    }
}