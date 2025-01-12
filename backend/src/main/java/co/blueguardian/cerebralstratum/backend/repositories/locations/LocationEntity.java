package co.blueguardian.cerebralstratum.backend.repositories.locations;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;

import java.time.LocalDateTime;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

import org.locationtech.jts.geom.Point;

@Entity
@Table(name = "locations")
@NamedQuery(
        name="Locations.findAll",
        query = "SELECT l FROM LocationEntity l WHERE l.device.id = :deviceId"
)
@NamedNativeQuery(
    name="Locations.latest",
    query = "SELECT DISTINCT ON (b.device_id) b.id, b.device_id, b.latitude, b.longitude "
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

    @Column(columnDefinition = "timestamp")
    private LocalDateTime timestamp;

    public LocationEntity() {
    }

    public LocationEntity(
        DeviceEntity device,
        Point coordinates,
        LocalDateTime timestamp
    ) {
        this.device = device;
        this.coordinates = coordinates;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
