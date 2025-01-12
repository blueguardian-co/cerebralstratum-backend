package co.blueguardian.cerebralstratum.backend.controllers.users;

import java.util.UUID;

public class DeleteUserRequest {
    public UUID keycloak_user_id;

    public DeleteUserRequest() {
    }

    public DeleteUserRequest(
            UUID keycloak_user_id
    ) {
        this.keycloak_user_id = keycloak_user_id;
    }
}