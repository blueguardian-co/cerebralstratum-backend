package co.blueguardian.cerebralstratum.backend.controllers.devices;

import co.blueguardian.cerebralstratum.backend.grpc.*;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.UUID;

@GrpcService
@Singleton
public class DeviceStreamService extends DeviceStreamServiceGrpc.DeviceStreamServiceImplBase {

    @Inject
    DeviceEventBroadcaster broadcaster;

    @Override
    public void streamDeviceUpdates(DeviceStreamRequest request, StreamObserver<DeviceUpdate> responseObserver) {
        UUID deviceId = UUID.fromString(request.getDeviceUuid());

        Multi<DeviceUpdate> updates = Multi.createBy().merging().streams(
                broadcaster.locationUpdatesFor(deviceId).map(GrpcDeviceUpdateMapper::toProto),
                broadcaster.statusUpdatesFor(deviceId).map(GrpcDeviceUpdateMapper::toProto),
                broadcaster.canBusUpdatesFor(deviceId).map(GrpcDeviceUpdateMapper::toProto)
        );

        updates.subscribe().with(
                responseObserver::onNext,
                responseObserver::onError,
                responseObserver::onCompleted
        );
    }
}
