package co.blueguardian.cerebralstratum.backend.controllers.users;

import java.time.LocalDateTime;
import java.util.UUID;

public class User {

    public UUID keycloak_user_id;
    public UUID keycloak_org_id;
    public LocalDateTime created;
    public Boolean subscription_active;
    public Integer subscription_discount;

    public User (
            UUID keycloak_user_id,
            UUID keycloak_org_id,
            LocalDateTime created,
            Boolean subscription_active,
            Integer subscription_discount
    ) {
        this.keycloak_user_id = keycloak_user_id;
        this.keycloak_org_id = keycloak_org_id;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;
    }
}
