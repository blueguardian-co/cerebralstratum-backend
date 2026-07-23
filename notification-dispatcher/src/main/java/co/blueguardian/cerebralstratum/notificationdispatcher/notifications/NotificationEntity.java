package co.blueguardian.cerebralstratum.notificationdispatcher.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @Column(unique = true)
    private UUID id;

    @Column(nullable = false)
    private UUID deviceId;

    @Column(nullable = false, length = 32)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    private LocalDateTime dispatchedAt;

    public NotificationEntity() {
    }

    public NotificationEntity(UUID id, UUID deviceId, String eventType, LocalDateTime occurredAt, LocalDateTime dispatchedAt) {
        this.id = id;
        this.deviceId = deviceId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.dispatchedAt = dispatchedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }
}