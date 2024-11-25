package repositories.locations;

import repositories.devices.DeviceEntity;

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
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;

@Entity
@Table(name = "locations")
@NamedQuery(
        name="Locations.findAll",
        query = "SELECT l FROM LocationEntity l WHERE l.device.id = :deviceId"
)
@NamedNativeQuery(
    name="Locations.latest",
    query = "DISTINCT ON (b.device_id) b.id, b.device_id, b.latitude, b.longtitude "
            + "FROM locations b "
            + "WHERE b.auction_id = :deviceId "
            + "ORDER BY b.timestamp;",
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

    @Column(columnDefinition="timestamp")
    private LocalDateTime timestamp;

    public LocationEntity() {
    }

    public LocationEntity(
        DeviceEntity device,
        LocalDateTime timestamp
    ) {
        this.device = device;
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
