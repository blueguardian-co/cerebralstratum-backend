package co.blueguardian.cerebralstratum.backend.controllers.groups;

import co.blueguardian.cerebralstratum.backend.controllers.users.UserResource;
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

@Path("/api/v1/authorisation/groups")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupsResource {

    private static final Logger LOG = Logger.getLogger(GroupsResource.class);

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm", defaultValue = "cerebral-stratum-backend")
    String KeycloakRealm;

    @GET
    @Path("/by-name/{group_name}/members")
    @PermissionsAllowed("member-of-group")
    public List<UserRepresentation> getGroupMembership(String group_name) {
        try {
            GroupRepresentation group = keycloak.realm(KeycloakRealm).getGroupByPath(group_name);
            return keycloak.realm(KeycloakRealm).groups().group(group.getId()).members();
        } catch (NotFoundException e) {
            throw new WebApplicationException("Group not found", Response.Status.NOT_FOUND);
        } catch (Exception e) {
            throw new WebApplicationException("An error occurred while fetching group membership", Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            LOG.warn("Issue with getGroupMembership request");
        }
    }

}
