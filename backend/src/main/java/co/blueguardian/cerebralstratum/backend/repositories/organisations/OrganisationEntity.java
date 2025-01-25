package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organisations")
@NamedQuery(
        name = "OrganisationEntity.findAll",
        query = "SELECT o FROM OrganisationEntity o ORDER BY o.id",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@NamedQuery(
        name = "OrganisationEntity.getOrganisationByKeycloakOrgId",
        query = "SELECT o FROM OrganisationEntity o WHERE o.id = :Keycloak_organisation_id",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@Cacheable
public class OrganisationEntity {

    @Id
    @Column(unique = true)
    private UUID id;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserEntity user;

    @Column
    private LocalDateTime created;

    public OrganisationEntity() {
    }

    public OrganisationEntity(
            UUID id,
            UserEntity user,
            LocalDateTime created
    ) {
        this.id = id;
        this.user = user;
        this.created = created;
    }


    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}