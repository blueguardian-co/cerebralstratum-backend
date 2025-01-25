package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(name = "locations")
@NamedQuery(
        name = "Locations.findAll",
        query = "SELECT l FROM LocationEntity l WHERE l.device.id = :deviceId"
)
@NamedNativeQuery(
        name = "Locations.latest",
        query = "SELECT DISTINCT ON (b.device_id) *"
                + "FROM locations b "
                + "WHERE b.device_id = :deviceId "
                + "ORDER BY b.device_id, b.timestamp DESC;",
        resultClass = LocationEntity.class
)
@Cacheable
public class LocationEntity {
    @Id
    @SequenceGenerator(name = "locationsSequence", sequenceName = "locations_id_seq", schema = "cerebralstratum", allocationSize = 1)
    @GeneratedValue(generator = "locationsSequence")
    private int id;
    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private DeviceEntity device;
    @Column(columnDefinition = "GEOMETRY(Point, 4326)")
    private Point coordinates;
    @Column
    private int update_frequency;
    @Column
    public int accuracy;
    @Column
    public double speed;
    @Column
    public double bearing;
    @Column(columnDefinition = "timestamp")
    private LocalDateTime timestamp;

    public LocationEntity() {
    }

    public LocationEntity(
            DeviceEntity device,
            Point coordinates,
            int update_frequency,
            int accuracy,
            double speed,
            double bearing,
            LocalDateTime timestamp
    ) {
        this.device = device;
        this.coordinates = coordinates;
        this.update_frequency = update_frequency;
        this.accuracy = accuracy;
        this.speed = speed;
        this.bearing = bearing;
        this.timestamp = timestamp;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public DeviceEntity getDevice() {
        return device;
    }

    public void setDevice(DeviceEntity device) {
        this.device = device;
    }

    public Point getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Point coordinates) {
        this.coordinates = coordinates;
    }

    public double getBearing() {
        return bearing;
    }

    public void setBearing(double bearing) {
        this.bearing = bearing;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public int getUpdate_frequency() {
        return update_frequency;
    }

    public void setUpdate_frequency(int update_frequency) {
        this.update_frequency = update_frequency;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
