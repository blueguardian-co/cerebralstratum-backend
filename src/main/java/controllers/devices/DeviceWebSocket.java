package controllers.devices;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;

import repositories.devices.DeviceRepository;
import repositories.locations.LocationRepository;
import repositories.users.UserRepository;
import controllers.locations.Location;

import io.quarkus.websockets.next.*;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@WebSocket(path = "/api/v1/devices/{device_uuid}/ws")
public class DeviceWebSocket {
    Integer device_id;
    public enum MessageType {CURRENT_LOCATION, DEVICE_NOTIFICATION, TEXT_MESSAGE}

    public record CurrentLocation(MessageType type, UUID device_uuid, Location location) {
    }
    public record DeviceNotification(MessageType type, UUID device_uuid, Set<Status> status) {
    }
    public record TextMessage(MessageType type, String message){
    }

    @Inject
    OpenConnections openConnections;

    @Inject
    DeviceRepository deviceRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    UserRepository userRepository;

    @RolesAllowed("Admins")
    @OnTextMessage(broadcast = true)
    public TextMessage onTextMessage(TextMessage message) {
        return message;
    }

    @OnClose
    public void onClose() {
        // Place a message on the bus?
    }

//    @Scheduled(every = "1m")
//    public void broadcastDeviceStatus() {
//        Device device = deviceRepository.getById(device_id);
//        for (WebSocketConnection c : openConnections) {
//            c.broadcast().sendTextAndAwait(
//                    new DeviceNotification(MessageType.DEVICE_NOTIFICATION, device.uuid, device.status)
//            );
//        }
//    }

    public void broadcastDeviceLocation() {
        Device device = deviceRepository.getById(device_id);
        Location location = locationRepository.getLatest(device_id);
        for (WebSocketConnection c : openConnections) {
            c.broadcast().sendTextAndAwait(
                    new CurrentLocation(MessageType.CURRENT_LOCATION, device.uuid, location)
            );
        }
    }
}