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
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwtToken;

    @GET
    @RolesAllowed("admin")
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @POST
    @Transactional
    @PermissionsAllowed("platform-admin")
    public Response create(CreateDeviceRequest request) {
        Device device = deviceRepository.create(request);
        return Response.ok(device).status(201).build();
    }

    @Path("{device_id}")
    @PUT
    @Transactional
    @PermissionsAllowed("member-of-group")
    public Response update(UUID device_id, UpdateDeviceRequest request) {
        Device device = deviceRepository.update(device_id, request);
        return Response.ok(device).status(200).build();
    }

    @DELETE
    @Path("{device_id}")
    @PermissionsAllowed("platform-admin")
    @Transactional
    public Response delete(UUID device_id) {
        Device device = deviceRepository.delete(device_id);
        return Response.ok(device).status(200).build();
    }

    @GET
    @Path("{device_id}")
    @PermissionsAllowed("platform-admin")
    public Device getDeviceById(UUID device_id) {
        Device device = deviceRepository.getById(device_id);
        if (device == null) {
            throw new WebApplicationException("Device with id of " + device_id + " does not exist.", 404);
        }
        return device;
    }

    @Path("{device_id}/register")
    @POST
    @Transactional
    public Response register(UUID device_id) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        try {
            Device device = deviceRepository.register(keycloak_user_id, device_id);
            return Response.ok(device).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("Device already registered. Please contact owner to unregister the device, and then try again.", Response.Status.UNAUTHORIZED);
        }
    }
    @Path("{device_id}/unregister")
    @POST
    @Transactional
    @PermissionsAllowed("member-of-group")
    public Response unregister(UUID device_id) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        try {
            Device device = deviceRepository.unregister(keycloak_user_id, device_id);
            return Response.ok(device).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("Failed to unregister device", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}