package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.Device;
import co.blueguardian.cerebralstratum.backend.controllers.devices.CreateDeviceRequest;
import co.blueguardian.cerebralstratum.backend.controllers.devices.UpdateDeviceRequest;

import java.util.List;
import java.util.UUID;

public interface DeviceRepository {

    public List<Device> findAll();

    public List<Device> findAllByUserId(UUID keycloak_user_id);

    public Device getById(UUID device_id);

    public Device create(CreateDeviceRequest request);

    public Device delete(UUID device_id);

    public Device update(UUID device_id, UpdateDeviceRequest request);

    public Device register(UUID keycloak_user_id, UUID device_id);

    public Device unregister(UUID keycloak_user_id, UUID device_id);
    
}
