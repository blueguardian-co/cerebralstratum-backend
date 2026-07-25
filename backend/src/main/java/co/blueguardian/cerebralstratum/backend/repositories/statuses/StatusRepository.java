package co.blueguardian.cerebralstratum.backend.repositories.statuses;

import co.blueguardian.cerebralstratum.utils.model.DeviceStatus;
import co.blueguardian.cerebralstratum.utils.model.Status;

import java.util.List;
import java.util.UUID;

public interface StatusRepository {

    public List<DeviceStatus> findAll(UUID device_uuid);

    public DeviceStatus getById(int status_id);

    public DeviceStatus getLatest(UUID device_uuid);

    public DeviceStatus record(UUID device_uuid, Status status);

    public int purgeExpired();

}