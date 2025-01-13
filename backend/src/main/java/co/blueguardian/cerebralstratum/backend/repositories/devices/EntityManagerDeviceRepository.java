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
            device.getUuid(),
            device.getName(),
            device.getDescription(),
            device.getRegistered(),
            device.getOwner().getKeycloakUserId(),
            device.getOrganisation().getKeycloakOrgId(),
            device.getImagePath(),
            device.getStatus()
        );
    }

    private DeviceEntity mapCreateRequestToEntity (CreateDeviceRequest request) {
        return new DeviceEntity(
            request.uuid,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }

    private void updateRequestToEntity (DeviceEntity device, UpdateDeviceRequest request) {
        if (!request.name.isEmpty()) {
            device.setName(request.name);
        }
        if (!request.description.isEmpty()) {
            device.setDescription(request.description);
        }
        if (request.keycloak_user_id != null) {
            UserEntity owner = entityManager.find(UserEntity.class, request.keycloak_user_id);
            device.setOwner(owner);
        }
        if (request.keycloak_org_id != null) {
            OrganisationEntity organisation = entityManager.find(OrganisationEntity.class, request.keycloak_org_id);
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

    public Device getById(UUID device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        return mapEntityToDevice(device);
    }

    @Transactional
    public Device create(CreateDeviceRequest request) {
        DeviceEntity newDevice = mapCreateRequestToEntity(request);
        entityManager.persist(newDevice);
        return mapEntityToDevice(newDevice);
    }

    @Transactional
    public Device delete(UUID device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        entityManager.remove(device);
        return mapEntityToDevice(device);
    }

    @Transactional
    public Device update(UUID device_id, UpdateDeviceRequest request) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        updateRequestToEntity(device, request);
        entityManager.merge(device);
        return mapEntityToDevice(device);
    }

    @Transactional
    public Device register(UUID keycloak_user_id, UUID device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
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
    public Device unregister(UUID keycloak_user_id, UUID device_id) {
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
