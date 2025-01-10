package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.Device;
import co.blueguardian.cerebralstratum.backend.controllers.devices.CreateDeviceRequest;
import co.blueguardian.cerebralstratum.backend.controllers.devices.RegisterDeviceRequest;
import co.blueguardian.cerebralstratum.backend.controllers.devices.UpdateDeviceRequest;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository {

    public List<Device> findAll();

    public Device getById(int id);

    public Device create(UUID keycloak_user_id, CreateDeviceRequest request);

    public Device delete(int auction_id);

    public Device update(Integer device_id, UpdateDeviceRequest request);

    public Device register(UUID keycloak_user_id, RegisterDeviceRequest request);

    public Device unregister(UUID keycloak_user_id, Integer device_id);
    
}
