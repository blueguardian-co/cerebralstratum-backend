package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.util.UUID;

public class UpdateOrganisationRequest {
    public int id;
    public UUID keycloak_org_id;
    public Integer owner_id;

    public UpdateOrganisationRequest() {
    }

    public UpdateOrganisationRequest(
            int id,
            UUID keycloak_org_id,
            Integer owner_id
    ) {
        this.id = id;
        this.keycloak_org_id = keycloak_org_id;
        this.owner_id = owner_id;
    }
}