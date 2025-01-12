package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.Table;

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

    @Column
    private UUID keycloak_user_id;

    @Column
    private LocalDateTime created;

    public OrganisationEntity() {
    }

    public OrganisationEntity(
            UUID keycloak_org_id,
            UUID keycloak_user_id,
            LocalDateTime created
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.keycloak_user_id = keycloak_user_id;
        this.created = created;
    }

    public UUID getKeycloakOrgId() {
        return keycloak_org_id;
    }

    public void setKeycloakOrgId(UUID keycloak_org_id) {
        this.keycloak_org_id = keycloak_org_id;
    }

    public UUID getKeycloakUserId() {
        return keycloak_user_id;
    }

    public void setKeycloakUserId(UUID keycloak_user_id) {
        this.keycloak_user_id = keycloak_user_id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}