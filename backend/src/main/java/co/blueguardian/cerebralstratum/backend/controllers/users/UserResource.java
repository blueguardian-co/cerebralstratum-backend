package co.blueguardian.cerebralstratum.backend.controllers.users;

import co.blueguardian.cerebralstratum.backend.repositories.users.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import io.quarkus.security.PermissionsAllowed;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.keycloak.admin.client.*;
import org.keycloak.representations.idm.*;
import io.quarkus.security.Authenticated;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/v1/authorisation/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final Logger LOG = Logger.getLogger(UserResource.class);

    @Inject
    JsonWebToken jwtToken;

    @Inject
    UserRepository userRepository;

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm", defaultValue = "cerebral-stratum-backend")
    String KeycloakRealm;

    @GET
    @RolesAllowed("admins")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @POST
    @Transactional
    @PermissionsAllowed("user-admin")
    @Path("by-id/{user_uuid}")
    public Response createUser(UUID user_uuid, CreateUserRequest request) {
        User user = userRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Transactional
    @PermissionsAllowed("user-admin")
    @Path("by-id/{user_uuid}")
    public Response deleteUser(UUID user_uuid, DeleteUserRequest request) {
        User user = userRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @PermissionsAllowed("user-admin")
    @Path("by-id/{user_uuid}")
    public User getUser(UUID user_uuid) {
        User user = userRepository.getById(user_uuid);
        if (user == null) {
            throw new WebApplicationException("User does not exist.", 404);
        }
        return user;
    }

    @GET
    @PermissionsAllowed("user-admin")
    @Path("by-id/{user_uuid}/organisations")
    public List<OrganizationRepresentation> getUsersOrganisations(UUID user_uuid) {
        User user = userRepository.getById(user_uuid);
        if (user == null) {
            throw new WebApplicationException("User does not exist.", 404);
        }
        return keycloak.realm(KeycloakRealm).organizations().members().getOrganizations(user_uuid.toString());
    }

    @GET
    @Path("me")
    public Response getMe() {
        User user = userRepository.getByKeycloakUserId(UUID.fromString(jwtToken.getClaim("sub")));
        if (user != null) {
            return Response.ok(user).status(200).build();
        } else {
            throw new WebApplicationException("User mapping does not exist.", 404);
        }
    }

    @POST
    @Path("me")
    @Transactional
    public Response createMe() {
        UUID keycloak_user_id = UUID.fromString(jwtToken.getClaim("sub"));
        UUID keycloak_org_id = UUID.fromString(jwtToken.getClaim("organization_id"));
        LocalDateTime created = LocalDateTime.now();
        User user = userRepository.create(new CreateUserRequest(
                keycloak_user_id,
                keycloak_org_id,
                created,
                false,
                0,
                0,
                0
                )
        );
        return Response.ok(user).status(201).build();
    }
}