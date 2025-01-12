package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateOrganisationRequest {
    public UUID keycloak_org_id;
    public UUID keycloak_user_id;
    public LocalDateTime created;

    public CreateOrganisationRequest() {
    }

    public CreateOrganisationRequest(
            UUID keycloak_org_id,
            UUID keycloak_user_id,
            LocalDateTime created
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.keycloak_user_id = keycloak_user_id;
        this.created = created;
    }
}