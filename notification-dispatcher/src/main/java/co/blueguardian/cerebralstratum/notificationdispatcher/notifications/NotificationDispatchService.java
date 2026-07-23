package co.blueguardian.cerebralstratum.notificationdispatcher.notifications;

import java.time.LocalDateTime;
import java.util.UUID;

import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NotificationDispatchService {

    private static final Logger LOG = Logger.getLogger(NotificationDispatchService.class);

    private final NotificationRepository repository;

    public NotificationDispatchService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Body only runs on a cache miss, so this executes at most once per (deviceId, eventType)
     * within the "notification-dispatch" Redis cache's expire-after-write window — that's the
     * dedupe/rate-limit behaviour this service exists to provide. occurredAt is deliberately
     * excluded from the cache key (only deviceId/eventType are @CacheKey) since every event
     * has a distinct timestamp and including it would defeat the dedupe entirely.
     */
    @CacheResult(cacheName = "notification-dispatch")
    public Boolean dispatch(@CacheKey UUID deviceId, @CacheKey String eventType, LocalDateTime occurredAt) {
        repository.record(UUID.randomUUID(), deviceId, eventType, occurredAt, LocalDateTime.now());
        LOG.infof("Dispatched notification for device=%s type=%s occurredAt=%s", deviceId, eventType, occurredAt);
        return Boolean.TRUE;
    }
}