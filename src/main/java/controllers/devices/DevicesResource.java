package controllers.devices;

import io.quarkus.security.identity.SecurityIdentity;
import repositories.devices.DeviceRepository;

import java.util.List;

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

@Path("/api/v1/devices")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DevicesResource {

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @RolesAllowed("admin")
    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }

    @POST
    @Transactional
    public Response create(CreateDeviceRequest request) {
        String username = securityIdentity.getPrincipal().getName();
        Device device = deviceRepository.create(username, request);
        return Response.ok(device).status(201).build();
    }

    @Path("{device_id}")
    @PUT
    @Transactional
    @RolesAllowed("admin")
    public Response update(Integer device_id, UpdateDeviceRequest request) {
        Device device = deviceRepository.update(device_id, request);
        return Response.ok(device).status(200).build();
    }

    @DELETE
    @Path("{device_id}")
    @RolesAllowed("admin")
    @Transactional
    public Response delete(Integer device_id) {
        Device device = deviceRepository.delete(device_id);
        return Response.ok(device).status(200).build();
    }

    @GET
    @Path("{device_id}")
    @RolesAllowed("admin")
    public Device getAuction(Integer device_id) {
        Device device = deviceRepository.getById(device_id);
        if (device == null) {
            throw new WebApplicationException("Device with id of " + device_id + " does not exist.", 404);
        }
        return device;
    }
}