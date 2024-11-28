package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;

import java.util.List;
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
        int device_id =  location.getDevice().getId();
        return new Location(
            location.getId(),
            device_id,
            location.getTimestamp()
        );
    }

    @Transactional
    public Location delete(int location_id) {
        LocationEntity location = entityManager.find(LocationEntity.class, location_id);
        entityManager.remove(location);
        return mapEntityToLocation(location);
    }

    public List<Location> findAll(int device_id) {
        return entityManager.createNamedQuery("Locations.findAll", LocationEntity.class)
            .setParameter("deviceId", device_id)
            .getResultList().stream().map(EntityManagerLocationRepository::mapEntityToLocation).collect(Collectors.toList());
    }

    public Location getById(int id) {
        LocationEntity location = entityManager.find(LocationEntity.class, id);
        return mapEntityToLocation(location);
    }

    public Location getLatest(int device_id) {
        return mapEntityToLocation(entityManager.createNamedQuery("Locations.latest", LocationEntity.class)
            .setParameter("deviceId", device_id)
            .getSingleResult()
        );
    }
}
