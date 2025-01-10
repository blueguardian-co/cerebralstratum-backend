package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
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
    @SequenceGenerator(
            name = "organisationSequence",
            sequenceName = "organisations_id_seq",
            schema = "cerebralstratum",
            allocationSize = 1,
            initialValue = 1
    )
    @GeneratedValue(generator = "organisationSequence")
    private Integer id;

    @Column(length = 255, unique = true)
    private UUID keycloak_org_id;

    @Column(unique = true)
    private Integer owner_id;

    @Column
    private LocalDateTime created;

    public OrganisationEntity() {
    }

    public OrganisationEntity(
        UUID keycloak_org_id,
        Integer owner_id,
        LocalDateTime created
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.owner_id = owner_id;
        this.created = created;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getKeycloakOrgId() {
        return keycloak_org_id;
    }

    public void setKeycloakOrgId(UUID keycloak_org_id) {
        this.keycloak_org_id = keycloak_org_id;
    }

    public Integer getOwnerId() {
        return owner_id;
    }

    public void setOwnerId(int owner_id) {
        this.owner_id = owner_id;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }
}