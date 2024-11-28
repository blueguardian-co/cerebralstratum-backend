package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.controllers.organisations.Organisation;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.CreateOrganisationRequest;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.DeleteOrganisationRequest;
import co.blueguardian.cerebralstratum.backend.controllers.organisations.UpdateOrganisationRequest;

import java.util.List;

public interface OrganisationRepository {

    public List<Organisation> findAll();

    public Organisation getById(int organisation_id);

    public Organisation getByName(String organisation_name);

    public Organisation create(CreateOrganisationRequest request);

    public Organisation delete(DeleteOrganisationRequest request);

    public Organisation update(UpdateOrganisationRequest request);
}