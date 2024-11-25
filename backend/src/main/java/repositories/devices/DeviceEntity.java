package repositories.devices;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import controllers.devices.Status;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import repositories.organisations.OrganisationEntity;
import repositories.users.UserEntity;

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
    @SequenceGenerator(
            name = "devicesSequence",
            sequenceName = "devices_id_seq",
            schema = "cerebralstratum",
            allocationSize = 1,
            initialValue = 1
    )
    @GeneratedValue(generator = "devicesSequence")
    private Integer id;

    @Column(unique = true)
    private UUID uuid;

    @Column(length = 255, unique = true)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(columnDefinition="timestamp")
    private LocalDateTime registered;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserEntity owner;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column
    private List<Integer> shared_users_read;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column
    private List<Integer> shared_users_modify;

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
        String name,
        UUID uuid,
        String description,
        LocalDateTime registered,
        UserEntity owner,
        List<Integer> shared_users_read,
        List<Integer> shared_users_modify,
        OrganisationEntity organisation,
        String image_path,
        Status status
    ) {
        this.name = name;
        this.uuid = uuid;
        this.description = description;
        this.registered = registered;
        this.owner = owner;
        this.shared_users_read = shared_users_read;
        this.shared_users_modify = shared_users_modify;
        this.organisation = organisation;
        this.image_path = image_path;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
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

    public List<Integer> getSharedUsersRead() {
        return shared_users_read;
    }

    public void setSharedUsersRead(List<Integer> shared_users_read) {
        this.shared_users_read = shared_users_read;
    }

    public List<Integer> getSharedUsersModify() {
        return shared_users_modify;
    }

    public void setSharedUsersModify(List<Integer> shared_users_modify) {
        this.shared_users_modify = shared_users_modify;
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