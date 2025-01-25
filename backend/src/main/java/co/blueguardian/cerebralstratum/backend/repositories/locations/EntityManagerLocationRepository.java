package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;

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
            location.getTimestamp()
        );
    }

    @Transactional
    public Location delete(int location_id) {
        LocationEntity location = entityManager.find(LocationEntity.class, location_id);
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
        return mapEntityToLocation(location);
    }

    public Location getLatest(UUID device_uuid) {
        return mapEntityToLocation(entityManager.createNamedQuery("Locations.latest", LocationEntity.class)
            .setParameter("deviceId", device_uuid)
            .getSingleResult()
        );
    }
}
