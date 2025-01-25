package co.blueguardian.cerebralstratum.backend.repositories.organisations;

import co.blueguardian.cerebralstratum.backend.controllers.organisations.*;

import java.util.List;
import java.util.UUID;

public interface OrganisationRepository {

    public List<Organisation> findAll();

    public Organisation getById(UUID organisation_id);

    public Organisation getByKeycloakOrgId(UUID keycloak_org_id);

    public Organisation create(CreateOrganisationRequest request);

    public Organisation delete(DeleteOrganisationRequest request);

    public Organisation update(UpdateOrganisationRequest request);
}