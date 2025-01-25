package co.blueguardian.cerebralstratum.backend.repositories.users;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@NamedQuery(name = "UserEntity.findAll", query = "SELECT u FROM UserEntity u ORDER BY u.id", hints = @QueryHint(name = "org.hibernate.cacheable", value = "false"))
@NamedQuery(name = "UserEntity.getByKeycloakUserId", query = "SELECT u FROM UserEntity u WHERE u.id = :keycloak_user_id", hints = @QueryHint(name = "org.hibernate.cacheable", value = "false"))
@Cacheable
public class UserEntity {

    @Id
    @Column(unique = true)
    private UUID id;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private OrganisationEntity organisation;

    @Column(columnDefinition = "timestamp")
    private LocalDateTime created;

    @Column
    private Boolean subscription_active;

    @Column
    private Integer subscription_discount;

    @Column
    private Integer subscription_entitlement;

    @Column
    private Integer subscription_used;

    public UserEntity() {
    }

    public UserEntity(
        UUID id,
        OrganisationEntity organisation,
        LocalDateTime created,
        Boolean subscription_active,
        Integer subscription_discount,
        Integer subscription_entitlement,
        Integer subscription_used
    ) {
        this.id = id;
        this.organisation = organisation;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;
        this.subscription_entitlement = subscription_entitlement;
        this.subscription_used = subscription_used;

    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
    public Integer getSubscriptionEntitlement() {
        return subscription_entitlement;
    }

    public void setSubscriptionEntitlement(Integer subscription_entitlement) {
        this.subscription_entitlement = subscription_entitlement;
    }

    public Integer getSubscriptionUsed() {
        return subscription_used;
    }

    public void setSubscriptionUsed(Integer subscription_used) {
        this.subscription_used = subscription_used;
    }

}