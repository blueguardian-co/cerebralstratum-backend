package Identity;

import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.PermissionChecker;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Set;

@Path("/api/v1/authorisation/groups")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Groups {
    @Inject
    Keycloak keycloak;

    @Inject
    JsonWebToken jwtToken;

    @GET
    @Path("{group_name}/membership")
    @PermissionsAllowed("member-of-group")
    public List<UserRepresentation> getGroupMembership(String group_name) {
        GroupRepresentation group = keycloak.realm("silent-auction").getGroupByPath(group_name);
        return keycloak.realm("silent-auction").groups().group(group.getId()).members();
    }

    @PermissionChecker("member-of-group")
    public boolean isMemberOfGroup(String group_name) {
        Set<String> groups = jwtToken.getGroups();
        return groups.contains(group_name);
    }

}
