package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.utils.PermissionCheckers;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Multiplexed device-telemetry streams: one SSE connection per component (location/status/
 * canbus) covering many devices, instead of DeviceServerSentEvents' one connection per device
 * per component — the latter hits browser per-host connection limits and server-side connection
 * overhead once a client watches more than a couple of devices.
 * <p>
 * Callers pass the device set explicitly via repeated {@code ?devices=} query params; each is
 * access-checked once at connect time through the same UMA path DeviceServerSentEvents uses
 * (there is no cheap in-process substitute — device grants live only in Keycloak, ADR-0005).
 * Devices the caller isn't authorized for are silently dropped rather than failing the whole
 * request. Because authorization is front-loaded at connect rather than re-checked per event, a
 * device whose access is revoked mid-connection keeps streaming until the client reconnects —
 * clients should reconnect periodically (recommended: every 5-10 min) to bound that window.
 * <p>
 * Unlike DeviceServerSentEvents (which emits the bare payload — the URL already tells the
 * client which device), these endpoints emit the {@link DeviceEvent} wrapper records
 * ({@code {"deviceId": ..., "location"|"status"|"canBus": ...}}) so a client watching several
 * devices on one connection can tell them apart. The device id lives only on Kafka's record
 * key upstream (DeviceEventBroadcaster) — none of GetLocationRequest/Status/CANBus carry it.
 */
@ApplicationScoped
@Authenticated
@Path("/api/v1/devices")
public class DeviceMultiplexedServerSentEvents {

    @Inject
    DeviceEventBroadcaster broadcaster;

    @Inject
    PermissionCheckers permissionCheckers;

    private Set<UUID> authorizedDevices(List<UUID> requested) {
        if (requested == null || requested.isEmpty()) {
            throw new BadRequestException("At least one device id is required, e.g. ?devices=<uuid>");
        }
        return requested.stream()
                .distinct()
                .filter(permissionCheckers::hasDeviceReadAccess)
                .collect(Collectors.toSet());
    }

    @GET
    @Path("locations")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<DeviceLocationEvent> broadcastLocations(@QueryParam("devices") List<UUID> devices) {
        Set<UUID> allowed = authorizedDevices(devices);
        return broadcaster.locationUpdatesFor(allowed);
    }

    @GET
    @Path("statuses")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<DeviceStatusEvent> broadcastStatuses(@QueryParam("devices") List<UUID> devices) {
        Set<UUID> allowed = authorizedDevices(devices);
        return broadcaster.statusUpdatesFor(allowed);
    }

    @GET
    @Path("canbuses")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<DeviceCanBusEvent> broadcastCanBuses(@QueryParam("devices") List<UUID> devices) {
        Set<UUID> allowed = authorizedDevices(devices);
        return broadcaster.canBusUpdatesFor(allowed);
    }
}
