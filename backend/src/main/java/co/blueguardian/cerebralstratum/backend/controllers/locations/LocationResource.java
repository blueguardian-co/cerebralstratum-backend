package co.blueguardian.cerebralstratum.backend.controllers.locations;

import co.blueguardian.cerebralstratum.backend.repositories.locations.LocationRepository;

import java.util.List;
import java.util.UUID;

import io.quarkus.security.PermissionsAllowed;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.Authenticated;

@Path("/api/v1/devices/{device_uuid}/locations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource {

    @Inject
    LocationRepository locationRepository;
    @RolesAllowed("admins")
    @GET
    public List<Location> getAllLocations(UUID device_uuid) {
        return locationRepository.findAll(device_uuid);
    }

    @DELETE
    @PermissionsAllowed("device-admin")
    @Transactional
    public Response delete(UUID device_uuid, Integer location_id) {
        Location location = locationRepository.delete(location_id);
        return Response.ok(location).status(201).build();
    }

    @GET
    @PermissionsAllowed("device-admin")
    @Path("{location_id}")
    public Location getSpecificLocation(UUID device_uuid, Integer location_id) {
        Location location = locationRepository.getById(location_id);
        if (location == null) {
            throw new WebApplicationException("Location with id of " + location_id + " does not exist.", 404);
        }
        return location;
    }

    @GET
    @PermissionsAllowed("device-admin")
    @Path("latest")
    public Location getLatest(UUID device_uuid) {
        try {
            return locationRepository.getLatest(device_uuid);
        } catch (Exception e) {
            throw new WebApplicationException("No locations exist for device.", 404);
        }
    }
}
