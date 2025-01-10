package co.blueguardian.cerebralstratum.backend.controllers.users;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationEntity;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {

    public int id;
    public UUID keycloak_user_id;
    public int organisation_id;
    public LocalDateTime created;
    public Boolean subscription_active;
    public Integer subscription_discount;

    public User (
            int id,
            UUID keycloak_user_id,
            int organisation_id,
            LocalDateTime created,
            Boolean subscription_active,
            Integer subscription_discount
    ) {
        this.id = id;
        this.keycloak_user_id = keycloak_user_id;
        this.organisation_id = organisation_id;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;
    }
}
