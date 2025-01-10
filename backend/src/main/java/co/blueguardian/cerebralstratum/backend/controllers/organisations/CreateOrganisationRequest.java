package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.time.LocalDateTime;
import java.util.UUID;

public class CreateOrganisationRequest {
    public UUID keycloak_org_id;
    public int owner;
    public LocalDateTime created;

    public CreateOrganisationRequest() {
    }

    public CreateOrganisationRequest(
            UUID keycloak_org_id,
            int owner,
            LocalDateTime created
    ) {
        this.keycloak_org_id = keycloak_org_id;
        this.owner = owner;
        this.created = created;
    }
}