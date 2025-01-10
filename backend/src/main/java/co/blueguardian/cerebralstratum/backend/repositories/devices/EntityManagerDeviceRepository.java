package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.*;
import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;
import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

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
            device.getOwner().getId(),
            device.getOrganisation().getId(),
            device.getImagePath(),
            device.getStatus()
        );
    }

    private DeviceEntity mapCreateRequestToEntity (UUID keycloak_user_id, CreateDeviceRequest request) {
        UUID uuid = UUID.randomUUID();
        LocalDateTime registered = LocalDateTime.now();
        UserEntity owner = entityManager.createNamedQuery("UserEntity.getByKeycloakUserId", UserEntity.class)
                .setParameter("keycloak_user_id", keycloak_user_id)
                .getSingleResult();
        OrganisationEntity organisation = null;
        Status status = new Status("Device Created", "Healthy", 1.00F);
        return new DeviceEntity(
            request.name,
            uuid,
            request.description,
            registered,
            owner,
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
    public Device create(UUID keycloak_user_id, CreateDeviceRequest request) {
        DeviceEntity newDevice = mapCreateRequestToEntity(keycloak_user_id, request);
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

    @Transactional
    public Device register(UUID keycloak_user_id, RegisterDeviceRequest request) {
        DeviceEntity device = entityManager.createNamedQuery("DeviceEntity.getDeviceByUUID", DeviceEntity.class)
                .setParameter("uuid", request.serial_number)
                .getSingleResult();
        if (device.getOwner() == null) {
            UserEntity owner = entityManager.createNamedQuery("UserEntity.getByKeycloakUserId", UserEntity.class)
                 .setParameter("keycloak_user_id", keycloak_user_id)
                 .getSingleResult();
            device.setOwner(owner);
            entityManager.persist(device);
            return mapEntityToDevice(device);
        } else {
            return null;
        }

    }

    @Transactional
    public Device unregister(UUID keycloak_user_id, Integer device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        UserEntity owner = device.getOwner();
        if (Objects.equals(owner.getKeycloakUserId(), keycloak_user_id)) {
            device.setOwner(null);
            entityManager.persist(device);
            return mapEntityToDevice(device);
        } else {
            return null;
        }

    }
    
}
