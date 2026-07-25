package co.blueguardian.cerebralstratum.backend.repositories.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionPolicy;
import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class RetentionPolicyRepositoryIT {

    private static final UUID UPDATER = UUID.fromString("00000000-0000-0000-0000-0000000000ff");

    @Inject
    RetentionPolicyRepository repository;

    @Inject
    EntityManager entityManager;

    private <T> T inTx(Supplier<T> work) {
        return QuarkusTransaction.requiringNew().call(work::get);
    }

    @BeforeEach
    void clearPolicies() {
        QuarkusTransaction.requiringNew().run(() ->
            entityManager.createQuery("DELETE FROM RetentionPolicyEntity").executeUpdate());
    }

    @Test
    void resolveFallsBackToConfiguredDefaultWhenNoPolicyExists() {
        int days = inTx(() -> repository.resolveRetentionDays(RetentionSubjectType.LOCATION, UUID.randomUUID()));
        // cerebral-stratum.retention.default-days defaults to 90 (see EntityManagerRetentionPolicyRepository).
        assertEquals(90, days);
    }

    @Test
    void globalDefaultUsedWhenNoExactSubjectPolicy() {
        repository.upsert(RetentionSubjectType.LOCATION, null, 45, "MANUAL", UPDATER);

        int days = inTx(() -> repository.resolveRetentionDays(RetentionSubjectType.LOCATION, UUID.randomUUID()));

        assertEquals(45, days);
    }

    @Test
    void exactSubjectPolicyWinsOverGlobalDefault() {
        UUID subject = UUID.randomUUID();
        repository.upsert(RetentionSubjectType.LOCATION, null, 30, "MANUAL", UPDATER);
        repository.upsert(RetentionSubjectType.LOCATION, subject, 7, "MANUAL", UPDATER);

        int days = inTx(() -> repository.resolveRetentionDays(RetentionSubjectType.LOCATION, subject));

        assertEquals(7, days);
    }

    @Test
    void upsertUpdatesExistingRowRatherThanDuplicating() {
        UUID subject = UUID.randomUUID();
        RetentionPolicy first = repository.upsert(RetentionSubjectType.LOCATION, subject, 10, "MANUAL", UPDATER);
        RetentionPolicy second = repository.upsert(RetentionSubjectType.LOCATION, subject, 20, "AUTO", UPDATER);

        assertEquals(first.id, second.id);
        assertEquals(20, second.retention_days);
        assertEquals("AUTO", second.source);

        List<RetentionPolicy> all = inTx(() -> repository.findAll(RetentionSubjectType.LOCATION));
        assertEquals(1, all.size());
    }

    @Test
    void findAllFiltersBySubjectType() {
        repository.upsert(RetentionSubjectType.LOCATION, UUID.randomUUID(), 10, "MANUAL", UPDATER);
        repository.upsert(RetentionSubjectType.STATUS, UUID.randomUUID(), 15, "MANUAL", UPDATER);

        assertEquals(1, inTx(() -> repository.findAll(RetentionSubjectType.LOCATION)).size());
        assertEquals(1, inTx(() -> repository.findAll(RetentionSubjectType.STATUS)).size());
    }
}
