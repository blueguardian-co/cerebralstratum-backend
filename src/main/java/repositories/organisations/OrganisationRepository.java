package repositories.organisations;

import java.util.List;
import controllers.organisations.Organisation;
import controllers.organisations.CreateOrganisationRequest;
import controllers.organisations.DeleteOrganisationRequest;
import controllers.organisations.UpdateOrganisationRequest;

public interface OrganisationRepository {

    public List<Organisation> findAll();

    public Organisation getById(int organisation_id);

    public Organisation getByName(String organisation_name);

    public Organisation create(CreateOrganisationRequest request);

    public Organisation delete(DeleteOrganisationRequest request);

    public Organisation update(UpdateOrganisationRequest request);
}