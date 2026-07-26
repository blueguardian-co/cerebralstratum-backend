package co.blueguardian.cerebralstratum.backend.controllers.statuses;

import co.blueguardian.cerebralstratum.utils.model.DeviceStatus;
import co.blueguardian.cerebralstratum.backend.repositories.statuses.StatusRepository;

import java.util.List;
import java.util.UUID;

import io.quarkus.security.PermissionsAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;

import io.quarkus.security.Authenticated;

@Path("/api/v1/devices/by-id/{device_uuid}/status")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatusResource {

    @Inject
    StatusRepository statusRepository;

    @PermissionsAllowed("device-read")
    @GET
    public List<DeviceStatus> getAllStatuses(UUID device_uuid) {
        return statusRepository.findAll(device_uuid);
    }

    @GET
    @PermissionsAllowed("device-read")
    @Path("{status_id}")
    public DeviceStatus getSpecificStatus(UUID device_uuid, Integer status_id) {
        DeviceStatus status = statusRepository.getById(status_id);
        if (status == null) {
            throw new WebApplicationException("Status with id of " + status_id + " does not exist.", 404);
        }
        return status;
    }

    @GET
    @PermissionsAllowed("device-read")
    @Path("latest")
    public DeviceStatus getLatest(UUID device_uuid) {
        try {
            return statusRepository.getLatest(device_uuid);
        } catch (Exception e) {
            throw new WebApplicationException("No statuses exist for device.", 404);
        }
    }
}