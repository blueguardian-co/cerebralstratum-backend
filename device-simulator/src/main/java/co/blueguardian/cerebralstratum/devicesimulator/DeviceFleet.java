package co.blueguardian.cerebralstratum.devicesimulator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Startup
@ApplicationScoped
class DeviceFleet {

    private static final Logger LOG = Logger.getLogger(DeviceFleet.class);
    private static final UUID DEFAULT_DEVICE_ID = UUID.fromString("e4bb7b63-6619-589b-98a3-549d0cedc8bc");

    @ConfigProperty(name = "simulator.device-count", defaultValue = "1")
    int deviceCount;

    @ConfigProperty(name = "simulator.origin-lat", defaultValue = "-27.4698")
    double originLat;

    @ConfigProperty(name = "simulator.origin-lon", defaultValue = "153.0251")
    double originLon;

    private List<SimulatedDevice> devices;

    @PostConstruct
    void init() {
        List<SimulatedDevice> fleet = new ArrayList<>();
        if (deviceCount <= 1) {
            fleet.add(new SimulatedDevice(DEFAULT_DEVICE_ID, originLat, originLon));
        } else {
            for (int i = 0; i < deviceCount; i++) {
                fleet.add(new SimulatedDevice(UUID.randomUUID(), originLat, originLon));
            }
        }
        this.devices = Collections.unmodifiableList(fleet);
        LOG.infof("Initialized %d simulated device(s): %s", devices.size(), devices.stream().map(d -> d.deviceId).toList());
    }

    List<SimulatedDevice> devices() {
        return devices;
    }
}
