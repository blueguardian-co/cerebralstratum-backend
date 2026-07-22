package co.blueguardian.cerebralstratum.devicesimulator;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import co.blueguardian.cerebralstratum.utils.messaging.CANBusMessage;
import co.blueguardian.cerebralstratum.utils.messaging.LocationMessage;
import co.blueguardian.cerebralstratum.utils.messaging.StatusMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
class DeviceTelemetryPublisher {

    private static final Logger LOG = Logger.getLogger(DeviceTelemetryPublisher.class);
    private static final String[] STATUS_SUMMARIES = {"nominal", "degraded", "nominal", "nominal"};

    @Inject
    DeviceFleet fleet;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "simulator.location-interval", defaultValue = "5s")
    Duration locationInterval;

    @Inject
    @Channel("/device/location")
    Emitter<String> locationEmitter;

    @Inject
    @Channel("/device/status")
    Emitter<String> statusEmitter;

    @Inject
    @Channel("/device/canbus")
    Emitter<String> canbusEmitter;

    @Scheduled(every = "{simulator.location-interval}")
    void publishLocations() {
        double intervalSeconds = locationInterval.toSeconds();
        for (SimulatedDevice device : fleet.devices()) {
            device.advanceLocation(intervalSeconds);
            LocationMessage message = new LocationMessage(
                    device.deviceId,
                    device.position,
                    (int) intervalSeconds,
                    5,
                    device.speedKph,
                    device.bearing,
                    LocalDateTime.now()
            );
            publish(locationEmitter, message, "location");
        }
    }

    @Scheduled(every = "{simulator.status-interval}")
    void publishStatuses() {
        for (SimulatedDevice device : fleet.devices()) {
            device.drainBattery();
            String summary = STATUS_SUMMARIES[ThreadLocalRandom.current().nextInt(STATUS_SUMMARIES.length)];
            StatusMessage message = new StatusMessage(
                    device.deviceId,
                    summary,
                    device.battery < 15f ? "WARNING" : "OK",
                    device.battery,
                    LocalDateTime.now()
            );
            publish(statusEmitter, message, "status");
        }
    }

    @Scheduled(every = "{simulator.canbus-interval}")
    void publishCanBusFrames() {
        for (SimulatedDevice device : fleet.devices()) {
            String frame = String.format("%08X#%016X", ThreadLocalRandom.current().nextInt(), ThreadLocalRandom.current().nextLong());
            CANBusMessage message = new CANBusMessage(device.deviceId, frame);
            publish(canbusEmitter, message, "canbus");
        }
    }

    private void publish(Emitter<String> emitter, Object message, String channel) {
        try {
            emitter.send(objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            LOG.errorf(e, "Failed to serialize %s message", channel);
        }
    }
}
