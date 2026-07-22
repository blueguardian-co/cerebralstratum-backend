package co.blueguardian.cerebralstratum.devicesimulator;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class SimulatedDevice {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final double METERS_PER_DEGREE_LATITUDE = 111_320.0;

    final UUID deviceId;
    Point position;
    double bearing;
    double speedKph;
    float battery;

    SimulatedDevice(UUID deviceId, double originLat, double originLon) {
        this.deviceId = deviceId;
        this.position = GEOMETRY_FACTORY.createPoint(new Coordinate(originLon, originLat));
        this.bearing = ThreadLocalRandom.current().nextDouble(0, 360);
        this.speedKph = ThreadLocalRandom.current().nextDouble(20, 80);
        this.battery = ThreadLocalRandom.current().nextFloat(60f, 100f);
    }

    void advanceLocation(double intervalSeconds) {
        bearing = (bearing + ThreadLocalRandom.current().nextDouble(-10, 10) + 360) % 360;
        speedKph = Math.max(0, speedKph + ThreadLocalRandom.current().nextDouble(-5, 5));

        double distanceMeters = speedKph * (1000.0 / 3600.0) * intervalSeconds;
        double bearingRadians = Math.toRadians(bearing);
        double latitude = position.getY();
        double longitude = position.getX();

        double deltaLat = (distanceMeters * Math.cos(bearingRadians)) / METERS_PER_DEGREE_LATITUDE;
        double metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * Math.cos(Math.toRadians(latitude));
        double deltaLon = metersPerDegreeLongitude == 0 ? 0 : (distanceMeters * Math.sin(bearingRadians)) / metersPerDegreeLongitude;

        position = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude + deltaLon, latitude + deltaLat));
    }

    void drainBattery() {
        battery -= ThreadLocalRandom.current().nextFloat(0.5f, 2f);
        if (battery <= 5f) {
            battery = 100f;
        }
    }
}
