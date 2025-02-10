package co.blueguardian.cerebralstratum.DeviceSimulator;

import co.blueguardian.cerebralstratum.backend.controllers.locations.Location;
import co.blueguardian.cerebralstratum.backend.controllers.devices.Status;
import co.blueguardian.cerebralstratum.utils.messaging.*;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DeviceSimulator{

    public int numberOfDevices = 1;

    public DeviceSimulator(){
    }

    public DeviceSimulator(
            int numberOfDevices
    ) {
        this.numberOfDevices = numberOfDevices;
    }

    public void initializeSimulatedDevices() {
        if (numberOfDevices == 1) {
            createSimulatedDevice(UUID.fromString("e4bb7b63-6619-589b-98a3-549d0cedc8bc"));
        } else if (numberOfDevices >=2 ) {
            for (int i = 0; i < numberOfDevices; i++) {
                createSimulatedDevice(UUID.randomUUID());
            }
        } else {
            System.out.println("Invalid number of devices specified. Please specify a number greater than 0.");
        }

    }

    @Outgoing("/device/location")
    public LocationMessage sendLocation(Location location) {
        return (
            new LocationMessage(
                location.device_id,
                location.coordinates,
                location.update_frequency,
                location.accuracy,
                location.speed,
                location.bearing,
                location.timestamp
            )
        );
    }

    @Outgoing("/device/status")
    public StatusMessage sendStatus(UUID device_id, Status status) {
        return (
                new StatusMessage(
                        device_id,
                        status.summary,
                        status.overall,
                        status.battery
                )
        );
    }

    private void createSimulatedDevice(UUID deviceId) {
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(() -> {
                    sendLocation(generateDeviceLocationPayload(deviceId));
                    sendStatus(deviceId, generateDeviceStatusPayload());
                }, 100, 60000, TimeUnit.MILLISECONDS);
    }

    private Location generateDeviceLocationPayload(UUID deviceId) {
        return new Location(

        );
    }

    private Status generateDeviceStatusPayload() {
        return new Status(

        );
    }

}
