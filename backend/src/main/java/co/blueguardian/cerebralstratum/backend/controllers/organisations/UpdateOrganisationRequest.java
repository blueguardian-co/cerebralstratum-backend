package co.blueguardian.cerebralstratum.backend.controllers.organisations;

public class UpdateOrganisationRequest {
    public int id;
    public String name;
    public Integer owner_id;

    public UpdateOrganisationRequest() {
    }

    public UpdateOrganisationRequest(
            int id,
            String name,
            Integer owner_id
    ) {
        this.id = id;
        this.name = name;
        this.owner_id = owner_id;
    }
}