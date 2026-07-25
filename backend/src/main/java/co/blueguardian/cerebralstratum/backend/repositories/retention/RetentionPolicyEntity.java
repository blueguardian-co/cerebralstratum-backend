package co.blueguardian.cerebralstratum.backend.repositories.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "retention_policies")
@NamedQuery(
        name = "RetentionPolicies.findAllByType",
        query = "SELECT r FROM RetentionPolicyEntity r WHERE r.subjectType = :subjectType ORDER BY r.subjectId"
)
@NamedQuery(
        name = "RetentionPolicies.findBySubject",
        query = "SELECT r FROM RetentionPolicyEntity r WHERE r.subjectType = :subjectType AND r.subjectId = :subjectId"
)
@NamedQuery(
        name = "RetentionPolicies.findGlobalDefault",
        query = "SELECT r FROM RetentionPolicyEntity r WHERE r.subjectType = :subjectType AND r.subjectId IS NULL"
)
@Cacheable
public class RetentionPolicyEntity {

    @Id
    @SequenceGenerator(name = "retentionPoliciesSequence", sequenceName = "retention_policies_id_seq", schema = "cerebralstratum", allocationSize = 1)
    @GeneratedValue(generator = "retentionPoliciesSequence")
    private int id;
    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", length = 32)
    private RetentionSubjectType subjectType;
    @Column(name = "subject_id")
    private UUID subjectId;
    @Column(name = "retention_days")
    private int retentionDays;
    @Column(length = 32)
    private String source;
    @Column(name = "updated_by")
    private UUID updatedBy;
    @Column(name = "updated_at", columnDefinition = "timestamp")
    private LocalDateTime updatedAt;

    public RetentionPolicyEntity() {
    }

    public RetentionPolicyEntity(
            RetentionSubjectType subjectType,
            UUID subjectId,
            int retentionDays,
            String source,
            UUID updatedBy,
            LocalDateTime updatedAt
    ) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.retentionDays = retentionDays;
        this.source = source;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public RetentionSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(RetentionSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public UUID getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(UUID subjectId) {
        this.subjectId = subjectId;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
