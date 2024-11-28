package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import java.time.LocalDateTime;

public class CreateOrganisationRequest {
    public String name;
    public int owner;
    public LocalDateTime created;

    public CreateOrganisationRequest() {
    }

    public CreateOrganisationRequest(
            String name,
            int owner,
            LocalDateTime created
    ) {
        this.name = name;
        this.owner = owner;
        this.created = created;
    }
}