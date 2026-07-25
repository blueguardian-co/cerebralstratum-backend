package co.blueguardian.cerebralstratum.backend.repositories.statuses;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;
import co.blueguardian.cerebralstratum.utils.model.DeviceStatus;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class StatusRepositoryIT {

    @Inject
    StatusRepository statusRepository;

    @Inject
    EntityManager entityManager;

    private UUID deviceId;

    private <T> T inTx(Supplier<T> work) {
        return QuarkusTransaction.requiringNew().call(work::get);
    }

    @BeforeEach
    void seedDevice() {
        deviceId = UUID.randomUUID();
        QuarkusTransaction.requiringNew().run(() -> {
            entityManager.createQuery("DELETE FROM StatusEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM RetentionPolicyEntity").executeUpdate();
            DeviceEntity device = new DeviceEntity();
            device.setId(deviceId);
            entityManager.persist(device);
        });
    }

    @Test
    void getByIdReturnsNullWhenMissing() {
        assertNull(statusRepository.getById(-1));
    }

    @Test
    void recordAppliesConfiguredDefaultRetention() {
        LocalDateTime now = LocalDateTime.now();
        DeviceStatus recorded = statusRepository.record(deviceId, new Status("ok", "green", 0.85f, now));

        assertNotNull(recorded.expires_at);
        // cerebral-stratum.retention.default-days defaults to 90.
        assertEquals(now.plusDays(90), recorded.expires_at);
    }

    @Test
    void purgeExpiredRemovesOnlyPastExpiry() {
        LocalDateTime past = LocalDateTime.now().minusDays(10);
        LocalDateTime future = LocalDateTime.now().plusDays(10);

        inTx(() -> {
            StatusEntity expired = new StatusEntity(
                entityManager.find(DeviceEntity.class, deviceId), "ok", "green", 0.5f, past);
            expired.setExpiresAt(past);
            entityManager.persist(expired);

            StatusEntity fresh = new StatusEntity(
                entityManager.find(DeviceEntity.class, deviceId), "ok", "green", 0.5f, past);
            fresh.setExpiresAt(future);
            entityManager.persist(fresh);
            return null;
        });

        int deleted = statusRepository.purgeExpired();

        assertEquals(1, deleted);
        assertTrue(inTx(() -> entityManager.createQuery("SELECT COUNT(s) FROM StatusEntity s", Long.class)
            .getSingleResult()) == 1);
    }
}
