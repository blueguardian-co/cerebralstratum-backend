package co.blueguardian.cerebralstratum.utils.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class RetentionPolicy {

    public int id;
    public RetentionSubjectType subject_type;
    public UUID subject_id;
    public int retention_days;
    public String source;
    public UUID updated_by;
    public LocalDateTime updated_at;

    public RetentionPolicy() {
    }

    public RetentionPolicy(
            int id,
            RetentionSubjectType subject_type,
            UUID subject_id,
            int retention_days,
            String source,
            UUID updated_by,
            LocalDateTime updated_at
    ) {
        this.id = id;
        this.subject_type = subject_type;
        this.subject_id = subject_id;
        this.retention_days = retention_days;
        this.source = source;
        this.updated_by = updated_by;
        this.updated_at = updated_at;
    }
}
