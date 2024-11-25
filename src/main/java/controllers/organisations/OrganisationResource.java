package controllers.organisations;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import repositories.organisations.OrganisationRepository;

import java.util.List;

// TODO: Manage Organisation members, etc using Keycloak Organisations

@Path("/api/v1")
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
    @Path("organisations")
    @RolesAllowed("admin")
    public List<Organisation> getAllOrganisations() {
        return organisationRepository.findAll();
    }

    @POST
    @Path("organisations")
    @Transactional
    @RolesAllowed("admin")
    public Response create(CreateOrganisationRequest request) {
        Organisation user = organisationRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Path("organisations")
    @Transactional
    @RolesAllowed("admin")
    public Response delete(DeleteOrganisationRequest request) {
        Organisation user = organisationRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("organisations/{organisation_id}")
    @RolesAllowed("admin")
    public Organisation getOrganisation(int organisation_id) {
        Organisation organisation = organisationRepository.getById(organisation_id);
        if (organisation == null) {
            throw new WebApplicationException("Organisation does not exist.", 404);
        }
        return organisation;
    }
}
