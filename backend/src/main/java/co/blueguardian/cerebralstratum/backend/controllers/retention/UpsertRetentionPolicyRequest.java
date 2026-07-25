package co.blueguardian.cerebralstratum.backend.controllers.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;

import java.util.UUID;

public class UpsertRetentionPolicyRequest {

    public RetentionSubjectType subject_type;
    public UUID subject_id;
    public int retention_days;

    public UpsertRetentionPolicyRequest() {
    }

    public UpsertRetentionPolicyRequest(RetentionSubjectType subject_type, UUID subject_id, int retention_days) {
        this.subject_type = subject_type;
        this.subject_id = subject_id;
        this.retention_days = retention_days;
    }
}
