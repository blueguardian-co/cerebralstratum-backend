package co.blueguardian.cerebralstratum.backend.repositories.statuses;

import co.blueguardian.cerebralstratum.backend.repositories.devices.DeviceEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_statuses")
@NamedQuery(
        name = "DeviceStatuses.findAll",
        query = "SELECT s FROM StatusEntity s WHERE s.device.id = :deviceId ORDER BY s.timestamp DESC"
)
@NamedNativeQuery(
        name = "DeviceStatuses.latest",
        query = "SELECT DISTINCT ON (b.device_id) *"
                + "FROM device_statuses b "
                + "WHERE b.device_id = :deviceId "
                + "ORDER BY b.device_id, b.timestamp DESC;",
        resultClass = StatusEntity.class
)
@Cacheable
public class StatusEntity {
    @Id
    @SequenceGenerator(name = "deviceStatusesSequence", sequenceName = "device_statuses_id_seq", schema = "cerebralstratum", allocationSize = 1)
    @GeneratedValue(generator = "deviceStatusesSequence")
    private int id;
    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private DeviceEntity device;
    @Column(length = 255)
    private String summary;
    @Column(length = 255)
    private String overall;
    @Column
    private float battery;
    @Column(columnDefinition = "timestamp")
    private LocalDateTime timestamp;
    @Column(name = "expires_at", columnDefinition = "timestamp")
    private LocalDateTime expiresAt;

    public StatusEntity() {
    }

    public StatusEntity(
            DeviceEntity device,
            String summary,
            String overall,
            float battery,
            LocalDateTime timestamp
    ) {
        this.device = device;
        this.summary = summary;
        this.overall = overall;
        this.battery = battery;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getOverall() {
        return overall;
    }

    public void setOverall(String overall) {
        this.overall = overall;
    }

    public float getBattery() {
        return battery;
    }

    public void setBattery(float battery) {
        this.battery = battery;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}