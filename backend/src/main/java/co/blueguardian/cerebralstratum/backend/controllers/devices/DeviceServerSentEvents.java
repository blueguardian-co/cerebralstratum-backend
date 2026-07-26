package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.utils.model.Status;

import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
@PermissionsAllowed("device-read")
@Path("/api/v1/devices/by-id/{device_uuid}")
public class DeviceServerSentEvents {

    @Inject
    DeviceEventBroadcaster broadcaster;

    @GET
    @Path("/location")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<GetLocationRequest> broadcastLocation(UUID device_uuid) {
        return broadcaster.locationUpdatesFor(device_uuid).map(DeviceLocationEvent::location);
    }

    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<Status> broadcastStatus(UUID device_uuid) {
        return broadcaster.statusUpdatesFor(device_uuid).map(DeviceStatusEvent::status);
    }

    @GET
    @Path("/canbus")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<CANBus> broadcastCANBus(UUID device_uuid) {
        return broadcaster.canBusUpdatesFor(device_uuid).map(DeviceCanBusEvent::canBus);
    }
}
