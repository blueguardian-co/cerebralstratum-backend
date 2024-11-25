package repositories.devices;

import java.util.List;
import controllers.devices.Device;
import controllers.devices.CreateDeviceRequest;
import controllers.devices.UpdateDeviceRequest;

public interface DeviceRepository {

    public List<Device> findAll();

    public Device getById(int id);

    public Device create(String username, CreateDeviceRequest request);

    public Device delete(int auction_id);

    public Device update(Integer device_id, UpdateDeviceRequest request);
    
}
