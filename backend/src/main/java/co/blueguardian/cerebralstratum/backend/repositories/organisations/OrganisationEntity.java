package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.repositories.users.UserEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "organisations")
@NamedQuery(
        name = "OrganisationEntity.findAll",
        query = "SELECT o FROM OrganisationEntity o ORDER BY o.keycloak_org_id",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@NamedQuery(
        name = "OrganisationEntity.getOrganisationByKeycloakOrgId",
        query = "SELECT o FROM OrganisationEntity o WHERE o.keycloak_org_id = :keycloak_org_id",
        hints = @QueryHint(
                name = "org.hibernate.cacheable",
                value = "false"
        )
)
@Cacheable
public class OrganisationEntity {

    @Id
    @Column(unique = true)
    private UUID keycloak_org_id;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private UserEntity owner;

    @Column
    private LocalDateTime created;

    public OrganisationEntity() {
    }

    public OrganisationEntity(
            UUID keycloak_org_id,
            UserEntity owner,
            LocalDateTime created
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.owner = owner;
        this.created = created;
    }


    public UUID getKeycloakOrgId() {
        return keycloak_org_id;
    }

    public void setKeycloakOrgId(UUID keycloak_org_id) {
        this.keycloak_org_id = keycloak_org_id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}