package controllers.organisations;

public class DeleteOrganisationRequest {
    public int id;

    public DeleteOrganisationRequest() {
    }

    public DeleteOrganisationRequest(
            int id
    ) {
        this.id = id;
    }
}