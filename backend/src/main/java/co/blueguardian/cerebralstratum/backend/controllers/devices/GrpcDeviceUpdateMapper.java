package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.controllers.locations.GetLocationRequest;
import co.blueguardian.cerebralstratum.backend.grpc.*;
import co.blueguardian.cerebralstratum.utils.model.Status;
import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** The only gRPC-specific translation code: DeviceEvent -> protobuf DeviceUpdate. */
final class GrpcDeviceUpdateMapper {

    private GrpcDeviceUpdateMapper() {
    }

    static DeviceUpdate toProto(DeviceLocationEvent event) {
        GetLocationRequest location = event.location();
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
                .setTimestamp(toTimestamp(location.timestamp))
                .build();

        return DeviceUpdate.newBuilder()
                .setType(MessageType.CURRENT_LOCATION)
                .setDeviceUuid(event.deviceId().toString())
                .setCurrentLocation(currentLocation)
                .build();
    }

    static DeviceUpdate toProto(DeviceStatusEvent event) {
        Status status = event.status();
        DeviceNotification notification = DeviceNotification.newBuilder()
                .setSummary(status.summary)
                .setOverall(status.overall)
                .setBattery(status.battery)
                .setTimestamp(toTimestamp(status.timestamp))
                .build();

        return DeviceUpdate.newBuilder()
                .setType(MessageType.DEVICE_NOTIFICATION)
                .setDeviceUuid(event.deviceId().toString())
                .setDeviceNotification(notification)
                .build();
    }

    static DeviceUpdate toProto(DeviceCanBusEvent event) {
        CANBusMessage canBusMessage = CANBusMessage.newBuilder()
                .setPayload(event.canBus().payload)
                .build();

        return DeviceUpdate.newBuilder()
                .setType(MessageType.CANBUS_MESSAGE)
                .setDeviceUuid(event.deviceId().toString())
                .setCanbusMessage(canBusMessage)
                .build();
    }

    private static Timestamp toTimestamp(LocalDateTime localDateTime) {
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
