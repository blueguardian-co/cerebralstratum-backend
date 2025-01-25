package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceRepository;

import io.quarkus.security.PermissionsAllowed;

import io.quarkus.security.identity.SecurityIdentity;

import java.util.List;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/v1/devices")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DevicesResource {

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    JsonWebToken jwtToken;

    @GET
    @RolesAllowed("admin")
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @POST
    @Path("{device_uuid}")
    @Transactional
    @PermissionsAllowed("platform-admin-devices")
    public Response create(CreateDeviceRequest request) {
        Device device = deviceRepository.create(request);
        return Response.ok(device).status(201).build();
    }

    @PUT
    @Path("{device_uuid}")
    @Transactional
    @PermissionsAllowed("member-of-device-group")
    public Response update(UUID device_uuid, UpdateDeviceRequest request) {
        Device device = deviceRepository.update(device_uuid, request);
        return Response.ok(device).status(200).build();
    }

    @DELETE
    @Path("{device_uuid}")
    @PermissionsAllowed("platform-admin-devices")
    @Transactional
    public Response delete(UUID device_uuid) {
        Device device = deviceRepository.delete(device_uuid);
        return Response.ok(device).status(200).build();
    }

    @GET
    @Path("{device_uuid}")
    @PermissionsAllowed("platform-admin-devices")
    public Device getDeviceById(UUID device_uuid) {
        Device device = deviceRepository.getById(device_uuid);
        if (device == null) {
            throw new WebApplicationException("Device with id of " + device_uuid + " does not exist.", 404);
        }
        return device;
    }

    @Path("{device_uuid}/register")
    @POST
    @Transactional
    public Response register(UUID device_uuid) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        try {
            Device device = deviceRepository.register(keycloak_user_id, device_uuid);
            return Response.ok(device).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("Device already registered. Please contact owner to unregister the device, and then try again.", Response.Status.UNAUTHORIZED);
        }
    }
    @Path("{device_uuid}/unregister")
    @POST
    @Transactional
    @PermissionsAllowed("member-of-device-group")
    public Response unregister(UUID device_uuid) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        try {
            Device device = deviceRepository.unregister(keycloak_user_id, device_uuid);
            return Response.ok(device).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("Failed to unregister device", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}