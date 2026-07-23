package co.blueguardian.cerebralstratum.notificationdispatcher.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationRepository {

    void record(UUID id, UUID deviceId, String eventType, LocalDateTime occurredAt, LocalDateTime dispatchedAt);

}