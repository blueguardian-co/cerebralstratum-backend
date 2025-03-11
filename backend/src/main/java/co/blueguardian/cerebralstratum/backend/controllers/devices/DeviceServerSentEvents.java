package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;
import co.blueguardian.cerebralstratum.utils.messaging.LocationMessage;

import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
@PermissionsAllowed("member-of-device-group")
@Path("/api/v1/devices/by-id/{device_uuid}")
public class DeviceServerSentEvents {
    private static final Logger LOG = Logger.getLogger(DeviceServerSentEvents.class);

    private final Multi<LocationMessage> locationMessages;
    private final Multi<DeviceNotification> deviceNotifications;
    private final Multi<CANBusMessage> canBusMessages;

    public DeviceServerSentEvents(
            @Channel("location") Multi<LocationMessage> locationMessages,
            @Channel("status") Multi<DeviceNotification> deviceNotifications,
            @Channel("canbus") Multi<CANBusMessage> canBusMessages
    ) {
        this.locationMessages = locationMessages;
        this.deviceNotifications = deviceNotifications;
        this.canBusMessages = canBusMessages;
    }

    public record DeviceNotification(int id, UUID device_uuid, Status status) {
        public DeviceNotification(UUID device_uuid, Status status) {
            this(-1,device_uuid,status);
        }
    }
    public record CANBusMessage(int id, UUID device_uuid, CANBus message) {
        public CANBusMessage(UUID device_uuid, CANBus message) {
            this(-1,device_uuid,message);
        }
    }

    @GET
    @Path("/location")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<LocationMessage> broadcastLocation(String device_uuid) {
        LOG.info("Subscribing to location messages for device: " + device_uuid);
        return this.locationMessages
                .filter(
                        currentMessage -> currentMessage.device_id
                                        .equals(
                                                UUID.fromString(device_uuid)
                                        )
                )
                .log();
    }

    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<DeviceNotification> broadcastStatus(String device_uuid) {
        return this.deviceNotifications;
    }

    @GET
    @Path("/canbus")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<CANBusMessage> broadcastCANBus(String device_uuid) {
        return this.canBusMessages;
    }
}