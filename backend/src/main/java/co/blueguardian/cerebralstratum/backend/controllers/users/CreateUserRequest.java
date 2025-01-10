package co.blueguardian.cerebralstratum.backend.controllers.users;

import java.util.UUID;

public class CreateUserRequest {
    public UUID keycloak_user_id;
    public Integer organisation_id;

    public CreateUserRequest() {
    }

    public CreateUserRequest(
            UUID keycloak_user_id,
            Integer organisation_id
    ) {
        this.keycloak_user_id = keycloak_user_id;
        this.organisation_id = organisation_id;
    }
}