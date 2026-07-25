package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;
import co.blueguardian.cerebralstratum.backend.repositories.retention.RetentionPolicyRepository;
import co.blueguardian.cerebralstratum.utils.model.Location;
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

@Default
    @ApplicationScoped
public class EntityManagerLocationRepository implements LocationRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    RetentionPolicyRepository retentionPolicyRepository;

    private static Location mapEntityToLocation (LocationEntity location) {
        UUID device_uuid =  location.getDevice().getId();
        return new Location(
            location.getId(),
            device_uuid,
            location.getCoordinates(),
            location.getUpdate_frequency(),
            location.getAccuracy(),
            location.getSpeed(),
            location.getBearing(),
            location.getTimestamp(),
            location.getExpiresAt()
        );
    }

    @Transactional
    public Location delete(int location_id) {
        LocationEntity location = entityManager.find(LocationEntity.class, location_id);
        if (location == null) {
            return null;
        }
        entityManager.remove(location);
        return mapEntityToLocation(location);
    }

    public List<Location> findAll(UUID device_uuid) {
        return entityManager.createNamedQuery("Locations.findAll", LocationEntity.class)
            .setParameter("deviceId", device_uuid)
            .getResultList().stream().map(EntityManagerLocationRepository::mapEntityToLocation).collect(Collectors.toList());
    }

    public Location getById(int id) {
        LocationEntity location = entityManager.find(LocationEntity.class, id);
        return location == null ? null : mapEntityToLocation(location);
    }

    public Location getLatest(UUID device_uuid) {
        return mapEntityToLocation(entityManager.createNamedQuery("Locations.latest", LocationEntity.class)
            .setParameter("deviceId", device_uuid)
            .getSingleResult()
        );
    }

    @Transactional
    public Location record(UUID device_uuid, GetLocationRequest request) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_uuid);
        UUID ownerId = device.getUser() != null ? device.getUser().getId() : null;
        int retentionDays = retentionPolicyRepository.resolveRetentionDays(RetentionSubjectType.LOCATION, ownerId);
        LocationEntity entity = new LocationEntity(
            device,
            request.coordinates,
            request.update_frequency,
            request.accuracy,
            request.speed,
            request.bearing,
            request.timestamp
        );
        entity.setExpiresAt(retentionDays > 0 ? request.timestamp.plusDays(retentionDays) : null);
        entityManager.persist(entity);
        return mapEntityToLocation(entity);
    }

    @Transactional
    public int purgeExpired() {
        return entityManager.createQuery("DELETE FROM LocationEntity l WHERE l.expiresAt IS NOT NULL AND l.expiresAt < :now")
            .setParameter("now", LocalDateTime.now())
            .executeUpdate();
    }
}
