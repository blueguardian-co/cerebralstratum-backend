package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;

import io.quarkus.security.PermissionsAllowed;
import io.smallrye.mutiny.Multi;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Channel;

import java.util.UUID;

/*
TODO:
- Add group `e4bb7b63-6619-589b-98a3-549d0cedc8bc` in Keycloak with `admin@exampl.com` as the only member
*/
@ApplicationScoped
@Path("/api/v1/devices/{device_uuid}")
public class DeviceServerSentEvents {

    private final Multi<CurrentLocationMessage> currentLocationMessages;
    private final Multi<DeviceNotification> deviceNotifications;
    private final Multi<CANBusMessage> canBusMessages;

    public DeviceServerSentEvents(
            @Channel("/device/location") Multi<CurrentLocationMessage> currentLocationMessages,
            @Channel("/device/status") Multi<DeviceNotification> deviceNotifications,
            @Channel("/device/canbus") Multi<CANBusMessage> canBusMessages
    ) {
        this.currentLocationMessages = currentLocationMessages;
        this.deviceNotifications = deviceNotifications;
        this.canBusMessages = canBusMessages;
    }

    public record CurrentLocationMessage(int id, UUID device_uuid, GetLocationRequest location) {
        public CurrentLocationMessage(UUID device_uuid, GetLocationRequest location) {
            this(-1,device_uuid,location);
        }
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

    @PermissionsAllowed("member-of-group")
    @GET
    @Path("/location")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<CurrentLocationMessage> broadcastLocation() {
        return this.currentLocationMessages;
    }

    @PermissionsAllowed("member-of-group")
    @GET
    @Path("/status")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<DeviceNotification> broadcastStatus() {
        return this.deviceNotifications;
    }

    @PermissionsAllowed("member-of-group")
    @GET
    @Path("/canbus")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public Multi<CANBusMessage> broadcastCANBus() {
        return this.canBusMessages;
    }
}