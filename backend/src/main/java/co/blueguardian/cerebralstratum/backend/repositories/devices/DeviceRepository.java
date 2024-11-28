package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.Device;
import co.blueguardian.cerebralstratum.backend.controllers.devices.CreateDeviceRequest;
import co.blueguardian.cerebralstratum.backend.controllers.devices.UpdateDeviceRequest;

import java.util.List;

public interface DeviceRepository {

    public List<Device> findAll();

    public Device getById(int id);

    public Device create(String username, CreateDeviceRequest request);

    public Device delete(int auction_id);

    public Device update(Integer device_id, UpdateDeviceRequest request);
    
}
