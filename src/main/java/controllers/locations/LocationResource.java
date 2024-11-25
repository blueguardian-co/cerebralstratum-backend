package controllers.locations;

import repositories.devices.DeviceRepository;
import controllers.devices.Device;
import repositories.locations.LocationRepository;

import java.util.List;

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
import jakarta.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/devices/{device_id}/locations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class LocationResource {

    @Inject
    SecurityIdentity securityIdentity;
    @Inject
    LocationRepository locationRepository;
    @Inject
    DeviceRepository deviceRepository;

    @GET
    public List<Location> getAllLocations(Integer device_id) {
        return locationRepository.findAll(device_id);
    }

    @DELETE
    @RolesAllowed("admin")
    @Transactional
    public Response delete(Integer device_id, Integer location_id) {
        Location location = locationRepository.delete(location_id);
        return Response.ok(location).status(201).build();
    }

    @GET
    @Path("{location_id}")
    public Location getSpecificLocation(Integer device_id, Integer location_id) {
        Location location = locationRepository.getById(location_id);
        if (location == null) {
            throw new WebApplicationException("Location with id of " + location_id + " does not exist.", 404);
        }
        return location;
    }

    @GET
    @Path("latest")
    public Location getLatest(Integer device_id) {
        try {
            return locationRepository.getLatest(device_id);
        } catch (Exception e) {
            throw new WebApplicationException("No locations exist for device.", 404);
        }
    }
}
