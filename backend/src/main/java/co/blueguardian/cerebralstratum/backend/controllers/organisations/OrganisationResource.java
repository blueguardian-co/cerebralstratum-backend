package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationRepository;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/v1/authorisation/organisations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganisationResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwt;

    @Inject
    OrganisationRepository organisationRepository;

    @GET
    @RolesAllowed("admin")
    public List<Organisation> getAllOrganisations() {
        return organisationRepository.findAll();
    }

    @POST
    @Transactional
    @RolesAllowed("admin")
    public Response create(CreateOrganisationRequest request) {
        Organisation user = organisationRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Transactional
    @RolesAllowed("admin")
    public Response delete(DeleteOrganisationRequest request) {
        Organisation user = organisationRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("{organisation_id}")
    @RolesAllowed("admin")
    public Organisation getOrganisation(int organisation_id) {
        Organisation organisation = organisationRepository.getById(organisation_id);
        if (organisation == null) {
            throw new WebApplicationException("Organisation does not exist.", 404);
        }
        return organisation;
    }
}
