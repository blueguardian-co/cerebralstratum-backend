package co.blueguardian.cerebralstratum.backend.repositories.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionPolicy;
import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Default
@ApplicationScoped
public class EntityManagerRetentionPolicyRepository implements RetentionPolicyRepository {

    @Inject
    EntityManager entityManager;

    @ConfigProperty(name = "cerebral-stratum.retention.default-days", defaultValue = "90")
    int defaultRetentionDays;

    private static RetentionPolicy mapEntityToRetentionPolicy(RetentionPolicyEntity policy) {
        return new RetentionPolicy(
            policy.getId(),
            policy.getSubjectType(),
            policy.getSubjectId(),
            policy.getRetentionDays(),
            policy.getSource(),
            policy.getUpdatedBy(),
            policy.getUpdatedAt()
        );
    }

    private RetentionPolicyEntity findExisting(RetentionSubjectType subjectType, UUID subjectId) {
        List<RetentionPolicyEntity> results = subjectId != null
            ? entityManager.createNamedQuery("RetentionPolicies.findBySubject", RetentionPolicyEntity.class)
                .setParameter("subjectType", subjectType)
                .setParameter("subjectId", subjectId)
                .getResultList()
            : entityManager.createNamedQuery("RetentionPolicies.findGlobalDefault", RetentionPolicyEntity.class)
                .setParameter("subjectType", subjectType)
                .getResultList();
        return results.isEmpty() ? null : results.get(0);
    }

    public List<RetentionPolicy> findAll(RetentionSubjectType subjectType) {
        return entityManager.createNamedQuery("RetentionPolicies.findAllByType", RetentionPolicyEntity.class)
            .setParameter("subjectType", subjectType)
            .getResultList().stream().map(EntityManagerRetentionPolicyRepository::mapEntityToRetentionPolicy).collect(Collectors.toList());
    }

    public RetentionPolicy getById(int id) {
        RetentionPolicyEntity policy = entityManager.find(RetentionPolicyEntity.class, id);
        return policy == null ? null : mapEntityToRetentionPolicy(policy);
    }

    public int resolveRetentionDays(RetentionSubjectType subjectType, UUID subjectId) {
        if (subjectId != null) {
            RetentionPolicyEntity exact = findExisting(subjectType, subjectId);
            if (exact != null) {
                return exact.getRetentionDays();
            }
        }
        RetentionPolicyEntity global = findExisting(subjectType, null);
        return global != null ? global.getRetentionDays() : defaultRetentionDays;
    }

    @Transactional
    public RetentionPolicy upsert(RetentionSubjectType subjectType, UUID subjectId, int retentionDays, String source, UUID updatedBy) {
        RetentionPolicyEntity existing = findExisting(subjectType, subjectId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setRetentionDays(retentionDays);
            existing.setSource(source);
            existing.setUpdatedBy(updatedBy);
            existing.setUpdatedAt(now);
            entityManager.merge(existing);
            return mapEntityToRetentionPolicy(existing);
        }
        RetentionPolicyEntity entity = new RetentionPolicyEntity(subjectType, subjectId, retentionDays, source, updatedBy, now);
        entityManager.persist(entity);
        return mapEntityToRetentionPolicy(entity);
    }

    @Transactional
    public RetentionPolicy delete(int id) {
        RetentionPolicyEntity policy = entityManager.find(RetentionPolicyEntity.class, id);
        if (policy == null) {
            return null;
        }
        entityManager.remove(policy);
        return mapEntityToRetentionPolicy(policy);
    }
}
