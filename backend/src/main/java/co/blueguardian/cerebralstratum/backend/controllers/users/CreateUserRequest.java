package co.blueguardian.cerebralstratum.backend.controllers.users;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateUserRequest {
    public UUID keycloak_user_id;
    public UUID keycloak_org_id;
    public LocalDateTime created;
    public Boolean subscription_active;
    public Integer subscription_discount;
    public Integer subscription_entitlement;
    public Integer subscription_used;

    public CreateUserRequest() {
    }

    public CreateUserRequest(
            UUID keycloak_user_id,
            UUID keycloak_org_id,
            LocalDateTime created,
            Boolean subscription_active,
            Integer subscription_discount,
            Integer subscription_entitlement,
            Integer subscription_used
    ) {
        this.keycloak_user_id = keycloak_user_id;
        this.keycloak_org_id = keycloak_org_id;
        this.created = created;
        this.subscription_active = subscription_active;
        this.subscription_discount = subscription_discount;
        this.subscription_entitlement = subscription_entitlement;
        this.subscription_used = subscription_used;
    }
}