package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.backend.grpc.*;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.quarkus.security.PermissionsAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.eclipse.microprofile.reactive.messaging.Incoming;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.google.protobuf.Timestamp;

@GrpcService
@ApplicationScoped
public class DeviceStreamService implements DeviceStreamServiceGrpc.AsyncService {

    // Map to track active streams per device UUID
    private final ConcurrentMap<String, ConcurrentMap<String, StreamObserver<DeviceUpdate>>> deviceStreams = new ConcurrentHashMap<>();

    @Override
    public void streamDeviceUpdates(DeviceStreamRequest request, StreamObserver<DeviceUpdate> responseObserver) {
        String deviceUuid = request.getDeviceUuid();

        // Generate a unique connection ID for this stream
        String connectionId = UUID.randomUUID().toString();

        // Register this stream for the device
        deviceStreams.computeIfAbsent(deviceUuid, k -> new ConcurrentHashMap<>())
                .put(connectionId, responseObserver);

        // Keep the stream open until the client disconnects or an error occurs.
        // The actual message sending happens in the Kafka consumer methods below
    }

    @Override
    public StreamObserver<TextMessage> streamTextMessages(StreamObserver<TextMessage> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(TextMessage message) {
                // Broadcast text message to all connected clients
                deviceStreams.values().forEach(connections ->
                        connections.values().forEach(observer -> {
                            try {
                                DeviceUpdate update = DeviceUpdate.newBuilder()
                                        .setType(MessageType.TEXT_MESSAGE)
                                        .setTextMessage(message)
                                        .build();
                                observer.onNext(update);
                            } catch (Exception e) {
                                // Handle disconnected clients
                                observer.onError(e);
                            }
                        })
                );
            }

            @Override
            public void onError(Throwable t) {
                responseObserver.onError(t);
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    // Kafka consumer for location updates
    @PermissionsAllowed("message-classifier")
    @Incoming("/device/location")
    public void consumeLocation(ConsumerRecord<UUID, GetLocationRequest> record) {
        String deviceUuid = record.key().toString();
        GetLocationRequest location = record.value();

        // Convert to a protobuf message
        CurrentLocation currentLocation = CurrentLocation.newBuilder()
                .setCoordinates(Coordinates.newBuilder()
                        .setX(location.coordinates.getX())
                        .setY(location.coordinates.getY())
                        .setZ(location.coordinates.getCoordinate().z)
                        .build())
                .setUpdateFrequency(location.update_frequency)
                .setAccuracy(location.accuracy)
                .setSpeed(location.speed)
                .setBearing(location.bearing)
                .setTimestamp(convertToTimestamp(location.timestamp))
                .build();

        DeviceUpdate update = DeviceUpdate.newBuilder()
                .setType(MessageType.CURRENT_LOCATION)
                .setDeviceUuid(deviceUuid)
                .setCurrentLocation(currentLocation)
                .build();

        // Send it to all streams listening to this device
        sendToDeviceStreams(deviceUuid, update);
    }

    // Kafka consumer for device status updates
    @PermissionsAllowed("message-classifier")
    @Incoming("/device/status")
    public void consumeStatus(ConsumerRecord<UUID, Status> record) {
        String deviceUuid = record.key().toString();
        Status status = record.value();

        // Convert to a protobuf message
        DeviceNotification notification = DeviceNotification.newBuilder()
                .setSummary(status.summary)
                .setOverall(status.overall)
                .setBattery(status.battery)
                .setTimestamp(convertToTimestamp(status.timestamp))
                .build();

        DeviceUpdate update = DeviceUpdate.newBuilder()
                .setType(MessageType.DEVICE_NOTIFICATION)
                .setDeviceUuid(deviceUuid)
                .setDeviceNotification(notification)
                .build();

        // Send it to all streams listening to this device
        sendToDeviceStreams(deviceUuid, update);
    }

    // Kafka consumer for CAN Bus messages
    @PermissionsAllowed("message-classifier")
    @Incoming("/device/canbus")
    public void consumeCANBus(ConsumerRecord<UUID, CANBus> record) {
        String deviceUuid = record.key().toString();
        CANBus canBus = record.value();

        // Convert to a protobuf message
        CANBusMessage canBusMessage = CANBusMessage.newBuilder()
                .setPayload(canBus.payload)
                .build();

        DeviceUpdate update = DeviceUpdate.newBuilder()
                .setType(MessageType.CANBUS_MESSAGE)
                .setDeviceUuid(deviceUuid)
                .setCanbusMessage(canBusMessage)
                .build();

        // Send it to all streams listening to this device
        sendToDeviceStreams(deviceUuid, update);
    }

    // Helper method to send updates to all streams for a specific device
    private void sendToDeviceStreams(String deviceUuid, DeviceUpdate update) {
        ConcurrentMap<String, StreamObserver<DeviceUpdate>> connections = deviceStreams.get(deviceUuid);
        if (connections != null) {
            connections.forEach((connectionId, observer) -> {
                try {
                    observer.onNext(update);
                } catch (Exception e) {
                    // Remove disconnected streams
                    connections.remove(connectionId);
                }
            });
        }
    }

    // Helper method to convert Java LocalDateTime to protobuf Timestamp
    private Timestamp convertToTimestamp(java.time.LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}
