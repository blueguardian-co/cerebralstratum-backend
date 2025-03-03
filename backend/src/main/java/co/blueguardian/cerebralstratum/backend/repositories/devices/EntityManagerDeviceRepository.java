package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.*;
import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;
import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

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
        UUID userId = device.getUser() != null ? device.getUser().getId() : null;
        UUID organisationId = device.getOrganisation() != null ? device.getOrganisation().getId() : null;
        return new Device(
            device.getId(),
            device.getName(),
            device.getDescription(),
            device.getRegistered(),
            userId,
            organisationId,
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
            device.setUser(owner);
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

    public List<Device> findAllByUserId(UUID keycloak_user_id) {
        return entityManager.createNamedQuery("DeviceEntity.findAllDevicesByUserId", DeviceEntity.class)
                .setParameter("keycloak_user_id", keycloak_user_id)
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
        if (device.getUser() == null) {
            UserEntity owner = entityManager.createNamedQuery("UserEntity.getByKeycloakUserId", UserEntity.class)
                 .setParameter("keycloak_user_id", keycloak_user_id)
                 .getSingleResult();
            device.setUser(owner);
            entityManager.persist(device);
            return mapEntityToDevice(device);
        } else {
            return null;
        }

    }

    @Transactional
    public Device unregister(UUID keycloak_user_id, UUID device_id) {
        DeviceEntity device = entityManager.find(DeviceEntity.class, device_id);
        UserEntity owner = device.getUser();
        if (Objects.equals(owner.getId(), keycloak_user_id)) {
            device.setUser(null);
            entityManager.persist(device);
            return mapEntityToDevice(device);
        } else {
            return null;
        }

    }
    
}
