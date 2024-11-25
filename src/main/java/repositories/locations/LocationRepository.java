package repositories.locations;

import java.util.List;
import controllers.locations.Location;

public interface LocationRepository {

    public List<Location> findAll(int device_id);

    public Location getById(int location_id);

    public Location getLatest(int device_id);

    public Location delete(int location_id);
    
}