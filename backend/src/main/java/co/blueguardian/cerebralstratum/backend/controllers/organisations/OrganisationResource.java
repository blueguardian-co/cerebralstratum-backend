package co.blueguardian.cerebralstratum.backend.controllers.organisations;

import co.blueguardian.cerebralstratum.backend.repositories.organisations.OrganisationRepository;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/authorisation/organisations")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrganisationResource {

    @Inject
    OrganisationRepository organisationRepository;

    @GET
    @RolesAllowed("admin")
    public List<Organisation> getAllOrganisations() {
        return organisationRepository.findAll();
    }

    @POST
    @Transactional
    @PermissionsAllowed("organisation-admin")
    @Path("{organisation_uuid}")
    public Response create(UUID organisation_uuid, CreateOrganisationRequest request) {
        Organisation user = organisationRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Transactional
    @PermissionsAllowed("organisation-admin")
    @Path("{organisation_uuid}")
    public Response delete(UUID organisation_uuid, DeleteOrganisationRequest request) {
        Organisation user = organisationRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @PermissionsAllowed("organisation-admin")
    @Path("{organisation_uuid}")
    public Organisation getOrganisation(UUID organisation_uuid) {
        Organisation organisation = organisationRepository.getById(organisation_uuid);
        if (organisation == null) {
            throw new WebApplicationException("Organisation does not exist.", 404);
        }
        return organisation;
    }
}
