package controllers.devices;

import controllers.locations.InboundLocation;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.websockets.next.*;
import io.quarkus.scheduler.Scheduled;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
@WebSocket(path = "/api/v1/devices/{device_uuid}/ws")
public class DeviceWebSocket {
    Integer device_id;
    public enum MessageType {CURRENT_LOCATION, DEVICE_NOTIFICATION, CANBUS, TEXT_MESSAGE}

    public record CurrentLocation(MessageType type, UUID device_uuid, InboundLocation location) {
    }
    public record DeviceNotification(MessageType type, UUID device_uuid, Set<Status> status) {
    }
    public record CANBus(MessageType type, UUID device_uuid, CANBus message) {
    }
    public record TextMessage(MessageType type, String message){
    }

    @Inject
    OpenConnections openConnections;

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
    @Incoming("kafka/device/location")
    public void consumeLocation(ConsumerRecord<UUID, InboundLocation> record) {
        for (WebSocketConnection c : openConnections) {
            c.broadcast().sendTextAndAwait(
                    new CurrentLocation(MessageType.CURRENT_LOCATION, record.key(), record.value())
            );
        }
    }
    @Incoming("kafka/device/status")
    public void consumeStatus(ConsumerRecord<UUID, Set<Status>> record) {
        for (WebSocketConnection c : openConnections) {
            c.broadcast().sendTextAndAwait(
                    new DeviceNotification(MessageType.DEVICE_NOTIFICATION, record.key(), record.value())
            );
        }
    }
    @Incoming("kafka/device/canbus")
    public void consumeCANBus(ConsumerRecord<UUID, CANBus> record) {
        for (WebSocketConnection c : openConnections) {
            c.broadcast().sendTextAndAwait(
                    new CANBus(MessageType.CANBUS, record.key(), record.value())
            );
        }
    }
}