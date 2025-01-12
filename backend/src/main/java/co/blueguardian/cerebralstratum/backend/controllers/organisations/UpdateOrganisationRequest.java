package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.util.UUID;

public class UpdateOrganisationRequest {
    public UUID keycloak_org_id;
    public UUID keycloak_user_id;

    public UpdateOrganisationRequest() {
    }

    public UpdateOrganisationRequest(
            UUID keycloak_org_id,
            UUID keycloak_user_id
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.keycloak_user_id = keycloak_user_id;
    }
}