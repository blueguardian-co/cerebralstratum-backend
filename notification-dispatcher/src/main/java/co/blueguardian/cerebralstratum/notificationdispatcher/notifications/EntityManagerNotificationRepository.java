package co.blueguardian.cerebralstratum.notificationdispatcher.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Default
@ApplicationScoped
public class EntityManagerNotificationRepository implements NotificationRepository {

    @Inject
    EntityManager entityManager;

    @Override
    @Transactional
    public void record(UUID id, UUID deviceId, String eventType, LocalDateTime occurredAt, LocalDateTime dispatchedAt) {
        entityManager.persist(new NotificationEntity(id, deviceId, eventType, occurredAt, dispatchedAt));
    }
}