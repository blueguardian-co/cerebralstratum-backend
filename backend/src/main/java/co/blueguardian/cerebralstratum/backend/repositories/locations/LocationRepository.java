package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;

import java.util.List;

public interface LocationRepository {

    public List<Location> findAll(int device_id);

    public Location getById(int location_id);

    public Location getLatest(int device_id);

    public Location delete(int location_id);
    
}