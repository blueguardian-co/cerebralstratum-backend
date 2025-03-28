package co.blueguardian.cerebralstratum.backend.controllers.users;

import java.util.UUID;

public class UpdateUserRequest {
    public UUID keycloak_user_id;
    public UUID keycloak_org_id;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(
            UUID keycloak_user_id,
            UUID keycloak_org_id
    ) {
        this.keycloak_user_id = keycloak_user_id;
        this.keycloak_org_id = keycloak_org_id;
    }
}