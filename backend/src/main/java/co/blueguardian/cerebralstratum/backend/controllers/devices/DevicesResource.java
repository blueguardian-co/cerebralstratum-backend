package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.users.User;

import io.quarkus.security.PermissionsAllowed;

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

import org.jboss.logging.Logger;

@Path("/api/v1/devices")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DevicesResource {

    private static final Logger LOG = Logger.getLogger(DevicesResource.class);

    @Inject
    co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceRepository deviceRepository;

    @Inject
    co.blueguardian.cerebralstratum.backend.repositories.users.UserRepository userRepository;

    @Inject
    org.eclipse.microprofile.jwt.JsonWebToken jwtToken;

    @GET
    @RolesAllowed("admins")
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @GET
    @Path("mine")
    public List<Device> getMyDevices() {
        UUID keycloak_user_id = UUID.fromString(jwtToken.getClaim("sub"));
        return deviceRepository.findAllByUserId(keycloak_user_id);
    }

    @POST
    @Path("by-id/{device_uuid}")
    @Transactional
    @RolesAllowed("admins")
    public Response create(CreateDeviceRequest request) {
        Device device = deviceRepository.create(request);
        return Response.ok(device).status(201).build();
    }

    @PUT
    @Path("by-id/{device_uuid}")
    @Transactional
    @PermissionsAllowed("member-of-device-group")
    public Response updateDevice(UUID device_uuid, UpdateDeviceRequest request) {
        Device device = deviceRepository.update(device_uuid, request);
        return Response.ok(device).status(200).build();
    }

    @DELETE
    @Path("by-id/{device_uuid}")
    @RolesAllowed("admins")
    @Transactional
    public Response deleteDevice(UUID device_uuid) {
        Device device = deviceRepository.delete(device_uuid);
        return Response.ok(device).status(200).build();
    }

    @GET
    @Path("by-id/{device_uuid}")
    @PermissionsAllowed("member-of-device-group")
    public Device getDeviceById(UUID device_uuid) {
        Device device = deviceRepository.getById(device_uuid);
        if (device == null) {
            throw new WebApplicationException("Device with id of " + device_uuid + " does not exist.", 404);
        }
        return device;
    }

    @Path("by-id/{device_uuid}/register")
    @POST
    @Transactional
    public Response registerDevice(UUID device_uuid) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        User user = userRepository.getById(keycloak_user_id);

        // Ensure user has subscription entitlements available for registration
        if (user.subscription_entitlement >= user.subscription_used) {
            try {
                Device device = deviceRepository.register(keycloak_user_id, device_uuid);
                user.subscription_used ++;
                return Response.ok(device).status(201).build();
            } catch (Exception e) {
                throw new WebApplicationException("Device already registered. Please contact owner to unregister the device, and then try again.", Response.Status.UNAUTHORIZED);
            }
        } else {
            throw new WebApplicationException("No entitlements available for registration.", Response.Status.UNAUTHORIZED);
        }
    }
    @Path("by-id/{device_uuid}/unregister")
    @POST
    @PermissionsAllowed("member-of-device-group")
    @Transactional
    public Response unregisterDevice(UUID device_uuid) {
        UUID keycloak_user_id = jwtToken.getClaim("sub");
        User user = userRepository.getById(keycloak_user_id);
        try {
            Device device = deviceRepository.unregister(keycloak_user_id, device_uuid);
            user.subscription_used --;
            return Response.ok(device).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("Failed to unregister device", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}