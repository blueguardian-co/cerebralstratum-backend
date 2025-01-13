package co.blueguardian.cerebralstratum.backend.repositories.devices;

import co.blueguardian.cerebralstratum.backend.controllers.devices.Status;
import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;
import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "devices")
@NamedQuery(
        name = "DeviceEntity.findAll",
        query = "SELECT a FROM DeviceEntity a ORDER BY a.name",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@NamedQuery(
        name = "DeviceEntity.getDeviceByName",
        query = "SELECT d FROM DeviceEntity d WHERE d.name = :name",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@Cacheable
public class DeviceEntity {

    @Id
    @Column(unique = true)
    private UUID uuid;

    @Column(length = 255)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(columnDefinition="timestamp")
    private LocalDateTime registered;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserEntity owner;

    @OneToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private OrganisationEntity organisation;

    @Column(length = 255)
    private String image_path;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Status status;

    public DeviceEntity() {
    }

    public DeviceEntity(
        UUID uuid,
        String name,
        String description,
        LocalDateTime registered,
        UserEntity owner,
        OrganisationEntity organisation,
        String image_path,
        Status status
    ) {
        this.uuid = uuid;
        this.name = name;
        this.description = description;
        this.registered = registered;
        this.owner = owner;
        this.organisation = organisation;
        this.image_path = image_path;
        this.status = status;
    }
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getRegistered() {
        return registered;
    }

    public void setRegistered(LocalDateTime registered) {
        this.registered = registered;
    }

    public UserEntity getOwner() {
        return this.owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public OrganisationEntity getOrganisation() {
        return this.organisation;
    }

    public void setOrganisation(OrganisationEntity organisation) {
        this.organisation = organisation;
    }

    public String getImagePath() {
        return image_path;
    }

    public void setImagePath(String image_path) {
        this.image_path = image_path;
    }

    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
}