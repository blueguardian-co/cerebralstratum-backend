package co.blueguardian.cerebralstratum.backend.controllers.groups;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

@Path("/api/v1/authorisation/groups")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GroupsResource {
    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm")
    String KeycloakRealm;

    @GET
    @Path("{group_name}/membership")
    @PermissionsAllowed("member-of-group")
    public List<UserRepresentation> getGroupMembership(String group_name) {
        GroupRepresentation group = keycloak.realm(KeycloakRealm).getGroupByPath(group_name);
        return keycloak.realm(KeycloakRealm).groups().group(group.getId()).members();
    }

}
