package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.InboundLocation;

import io.quarkus.security.PermissionsAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.websockets.next.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.util.UUID;

@ApplicationScoped
@WebSocket(path = "/api/v1/devices/{device_uuid}/ws")
public class DeviceWebSocket {
    Integer device_id;
    public enum MessageType {CURRENT_LOCATION, DEVICE_NOTIFICATION, CANBUS_MESSAGE, TEXT_MESSAGE}

    public record CurrentLocationMessage(MessageType type, UUID device_uuid, InboundLocation location) {
    }
    public record DeviceNotification(MessageType type, UUID device_uuid, Status status) {
    }
    public record CANBusMessage(MessageType type, UUID device_uuid, CANBus message) {
    }
    public record TextMessage(MessageType type, String message){
    }

    @Inject
    OpenConnections openConnections;

    @PermissionsAllowed("platform-admin")
    @OnTextMessage(broadcast = true)
    public TextMessage onTextMessage(TextMessage message) {
        return message;
    }

//    @OnClose
//    public void onClose() {
//        // Place a message on the bus?
//    }

    @PermissionsAllowed("message-classifier")
    @Incoming("kafka/device/location")
    public void consumeLocation(ConsumerRecord<UUID, InboundLocation> record) {
        for (WebSocketConnection c : openConnections) {
            try {
                c.sendTextAndAwait(
                        new CurrentLocationMessage(MessageType.CURRENT_LOCATION, record.key(), record.value())
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    @PermissionsAllowed("message-classifier")
    @Incoming("kafka/device/status")
    public void consumeStatus(ConsumerRecord<UUID, Status> record) {
        for (WebSocketConnection c : openConnections) {
            c.sendTextAndAwait(
                    new DeviceNotification(MessageType.DEVICE_NOTIFICATION, record.key(), record.value())
            );
        }
    }
    @PermissionsAllowed("message-classifier")
    @Incoming("kafka/device/canbus")
    public void consumeCANBus(ConsumerRecord<UUID, CANBus> record) {
        for (WebSocketConnection c : openConnections) {
            c.sendTextAndAwait(
                    new CANBusMessage(MessageType.CANBUS_MESSAGE, record.key(), record.value())
            );
        }
    }
}