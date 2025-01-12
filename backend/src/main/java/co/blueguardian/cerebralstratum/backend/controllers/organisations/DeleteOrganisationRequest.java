package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.util.UUID;

public class DeleteOrganisationRequest {
    public UUID keycloak_org_id;

    public DeleteOrganisationRequest() {
    }

    public DeleteOrganisationRequest(
            UUID keycloak_org_id
    ) {
        this.keycloak_org_id = keycloak_org_id;
    }
}