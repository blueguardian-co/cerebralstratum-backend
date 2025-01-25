package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;

import java.util.List;
import java.util.UUID;

public interface LocationRepository {

    public List<Location> findAll(UUID device_uuid);

    public Location getById(int location_id);

    public Location getLatest(UUID device_uuid);

    public Location delete(int location_id);
    
}