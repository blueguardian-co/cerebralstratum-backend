package repositories.devices;

import controllers.devices.Device;
import controllers.devices.Status;
import controllers.devices.CreateDeviceRequest;
import controllers.devices.UpdateDeviceRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import repositories.organisations.OrganisationEntity;
import repositories.users.UserEntity;

@Default
@ApplicationScoped
public class EntityManagerDeviceRepository implements DeviceRepository {

    @Inject
    EntityManager entityManager;

    private static Device mapEntityToDevice (DeviceEntity device) {
        return new Device(
            device.getId(),
            device.getName(),
            device.getUuid(),
            device.getDescription(),
            device.getRegistered(),
            device.getOwner(),
            device.getSharedUsersRead(),
            device.getSharedUsersModify(),
            device.getOrganisation(),
            device.getImagePath(),
            device.getStatus()
        );
    }

    private DeviceEntity mapCreateRequestToEntity (String username, CreateDeviceRequest request) {
        UUID uuid = UUID.randomUUID();
        LocalDateTime registered = LocalDateTime.now();
        UserEntity owner = entityManager.createNamedQuery("UserEntity.getUser", UserEntity.class)
                .setParameter("username", username)
                .getSingleResult();
        OrganisationEntity organisation = null;
        List<Integer> sharedUsersRead = new ArrayList<>();
        List<Integer> sharedUsersModify = new ArrayList<>();
        Status status = new Status("Device Created", "Healthy", 1.00F);
        return new DeviceEntity(
            request.name,
            uuid,
            request.description,
            registered,
            owner,
            sharedUsersRead,
            sharedUsersModify,
            organisation,
            request.image_path,
            status
        );
    }

    private void updateRequestToEntity (DeviceEntity device, UpdateDeviceRequest request) {
        if (!request.name.isEmpty()) {
            device.setName(request.name);
        }
        if (!request.description.isEmpty()) {
            device.setDescription(request.description);
        }
        if (request.owner_id != null) {
            UserEntity owner = entityManager.find(UserEntity.class, request.owner_id);
            device.setOwner(owner);
        }
        if (request.shared_users_read != null) {
            device.setSharedUsersRead(request.shared_users_read);
        }
        if (request.shared_users_modify != null) {
            device.setSharedUsersModify(request.shared_users_modify);
        }
        if (request.organisation_id != null) {
            OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.organisation_id);
            device.setOrganisation(organisation);
        }
        if (!request.image_path.isEmpty()) {
            device.setImagePath(request.image_path);
        }
    }

    public List<Device> findAll() {
        return entityManager.createNamedQuery("DeviceEntity.findAll", DeviceEntity.class)
            .getResultList().stream().map(EntityManagerDeviceRepository::mapEntityToDevice).collect(Collectors.toList());
    }

    public Device getById(int id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, id);
        return mapEntityToDevice(device);
    }

    @Transactional
    public Device create(String username, CreateDeviceRequest request) {
        DeviceEntity newDevice = mapCreateRequestToEntity(username, request);
        entityManager.persist(newDevice);
        return mapEntityToDevice(newDevice);
    }

    @Transactional
    public Device delete(int device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        entityManager.remove(device);
        return mapEntityToDevice(device);
    }

    @Transactional
    public Device update(Integer device_id, UpdateDeviceRequest request) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        updateRequestToEntity(device, request);
        entityManager.merge(device);
        return mapEntityToDevice(device);
    }
    
}
