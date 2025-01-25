package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;

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
    public enum MessageType {CURRENT_LOCATION, DEVICE_NOTIFICATION, CANBUS_MESSAGE, TEXT_MESSAGE}

    public record CurrentLocationMessage(MessageType type, UUID device_uuid, GetLocationRequest location) {
    }
    public record DeviceNotification(MessageType type, UUID device_uuid, Status status) {
    }
    public record CANBusMessage(MessageType type, UUID device_uuid, CANBus message) {
    }
    public record TextMessage(MessageType type, String message){
    }

    @Inject
    io.quarkus.websockets.next.OpenConnections openConnections;

//    @Inject
//    io.quarkus.websockets.next.WebSocketConnection connection;
//
//    String device_uuid = connection.pathParam("device_uuid");

//    @OnClose
//    public void onClose() {
//        // Place a message on the bus?
//    }
    @OnTextMessage(broadcast = true)
    public TextMessage onTextMessage(TextMessage message) {
        return message;
    }
//    @PermissionsAllowed("message-classifier")
//    @Incoming("/device/location")
//    public void consumeLocation(String device_uuid, ConsumerRecord<UUID, GetLocationRequest> record) {
//        for (WebSocketConnection c : openConnections) {
//            try {
//                c.sendTextAndAwait(
//                        new CurrentLocationMessage(MessageType.CURRENT_LOCATION, record.key(), record.value())
//                );
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
//    }
//    @PermissionsAllowed("message-classifier")
//    @Incoming("/device/status")
//    public void consumeStatus(String device_uuid, ConsumerRecord<UUID, Status> record) {
//        for (WebSocketConnection c : openConnections) {
//            c.sendTextAndAwait(
//                    new DeviceNotification(MessageType.DEVICE_NOTIFICATION, record.key(), record.value())
//            );
//        }
//    }
//    @PermissionsAllowed("message-classifier")
//    @Incoming("/device/canbus")
//    public void consumeCANBus(String device_uuid, ConsumerRecord<UUID, CANBus> record) {
//        for (WebSocketConnection c : openConnections) {
//            c.sendTextAndAwait(
//                    new CANBusMessage(MessageType.CANBUS_MESSAGE, record.key(), record.value())
//            );
//        }
//    }
}