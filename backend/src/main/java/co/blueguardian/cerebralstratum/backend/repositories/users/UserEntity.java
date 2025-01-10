package co.blueguardian.cerebralstratum.backend.repositories.users;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@NamedQuery(name = "UserEntity.findAll", query = "SELECT u FROM UserEntity u ORDER BY u.keycloak_user_id", hints = @QueryHint(name = "org.hibernate.cacheable", value = "false"))
@NamedQuery(name = "UserEntity.getByKeycloakUserId", query = "SELECT u FROM UserEntity u WHERE u.keycloak_user_id = :keycloak_user_id", hints = @QueryHint(name = "org.hibernate.cacheable", value = "false"))
@Cacheable
public class UserEntity {

    @Id
    @SequenceGenerator(
            name = "userSequence",
            sequenceName = "users_id_seq",
            schema = "cerebralstratum",
            allocationSize = 1,
            initialValue = 1
    )
    @GeneratedValue(generator = "userSequence")
    private Integer id;

    @Column(unique = true)
    private UUID keycloak_user_id;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private OrganisationEntity organisation;

    @Column(columnDefinition = "timestamp")
    private LocalDateTime created;

    @Column
    private Boolean subscription_active;

    @Column
    private Integer subscription_discount;

    public UserEntity() {
    }

    public UserEntity(
        UUID keycloak_user_id,
        OrganisationEntity organisation,
        LocalDateTime created,
        Boolean subscription_active,
        Integer subscription_discount


    ) {
        this.keycloak_user_id = keycloak_user_id;
        this.organisation = organisation;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getKeycloakUserId() {
        return keycloak_user_id;
    }

    public void setKeycloakUserId(UUID keycloak_user_id) {
        this.keycloak_user_id = keycloak_user_id;
    }

    public OrganisationEntity getOrganisation() {
        return this.organisation;
    }

    public void setOrganisation(OrganisationEntity organisation) {
        this.organisation = organisation;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Boolean getSubscriptionActive() {
        return this.subscription_active;
    }

    public void setSubscriptionActive(Boolean subscription_active) {
        this.subscription_active = subscription_active;
    }

    public Integer getSubscriptionDiscount() {
        return this.subscription_discount;
    }

    public void setSubscriptionDiscount(Integer subscription_discount) {
        this.subscription_discount = subscription_discount;
    }

}