package co.blueguardian.cerebralstratum.backend.repositories.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionPolicy;
import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;

import java.util.List;
import java.util.UUID;

public interface RetentionPolicyRepository {

    public List<RetentionPolicy> findAll(RetentionSubjectType subjectType);

    public RetentionPolicy getById(int id);

    public int resolveRetentionDays(RetentionSubjectType subjectType, UUID subjectId);

    public RetentionPolicy upsert(RetentionSubjectType subjectType, UUID subjectId, int retentionDays, String source, UUID updatedBy);

    public RetentionPolicy delete(int id);

}
