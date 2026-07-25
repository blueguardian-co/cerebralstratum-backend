package co.blueguardian.cerebralstratum.backend.repositories.statuses;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;
import co.blueguardian.cerebralstratum.backend.repositories.retention.RetentionPolicyRepository;
import co.blueguardian.cerebralstratum.utils.model.DeviceStatus;
import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Default
@ApplicationScoped
public class EntityManagerStatusRepository implements StatusRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    RetentionPolicyRepository retentionPolicyRepository;

    private static DeviceStatus mapEntityToDeviceStatus(StatusEntity status) {
        UUID device_uuid = status.getDevice().getId();
        return new DeviceStatus(
            status.getId(),
            device_uuid,
            status.getSummary(),
            status.getOverall(),
            status.getBattery(),
            status.getTimestamp(),
            status.getExpiresAt()
        );
    }

    public List<DeviceStatus> findAll(UUID device_uuid) {
        return entityManager.createNamedQuery("DeviceStatuses.findAll", StatusEntity.class)
            .setParameter("deviceId", device_uuid)
            .getResultList().stream().map(EntityManagerStatusRepository::mapEntityToDeviceStatus).collect(Collectors.toList());
    }

    public DeviceStatus getById(int status_id) {
        StatusEntity status = entityManager.find(StatusEntity.class, status_id);
        return status == null ? null : mapEntityToDeviceStatus(status);
    }

    public DeviceStatus getLatest(UUID device_uuid) {
        return mapEntityToDeviceStatus(entityManager.createNamedQuery("DeviceStatuses.latest", StatusEntity.class)
            .setParameter("deviceId", device_uuid)
            .getSingleResult()
        );
    }

    @Transactional
    public DeviceStatus record(UUID device_uuid, Status status) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_uuid);
        UUID ownerId = device.getUser() != null ? device.getUser().getId() : null;
        int retentionDays = retentionPolicyRepository.resolveRetentionDays(RetentionSubjectType.STATUS, ownerId);
        StatusEntity entity = new StatusEntity(device, status.summary, status.overall, status.battery, status.timestamp);
        entity.setExpiresAt(retentionDays > 0 ? status.timestamp.plusDays(retentionDays) : null);
        entityManager.persist(entity);
        return mapEntityToDeviceStatus(entity);
    }

    @Transactional
    public int purgeExpired() {
        return entityManager.createQuery("DELETE FROM StatusEntity s WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now")
            .setParameter("now", LocalDateTime.now())
            .executeUpdate();
    }
}