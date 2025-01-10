package co.blueguardian.cerebralstratum.backend.controllers.users;

import co.blueguardian.cerebralstratum.backend.repositories.users.UserRepository;

import java.util.List;

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
import jakarta.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/v1/authorisation/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    JsonWebToken jwtToken;

    @Inject
    UserRepository userRepository;

    @GET
    @RolesAllowed("admin")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @POST
    @Transactional
    @RolesAllowed("admin")
    public Response create(CreateUserRequest request) {
        User user = userRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Transactional
    @RolesAllowed("admin")
    public Response delete(DeleteUserRequest request) {
        User user = userRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("{user_id}")
    @RolesAllowed("admin")
    public User getUser(Integer user_id) {
        User user = userRepository.getById(user_id);
        if (user == null) {
            throw new WebApplicationException("User does not exist.", 404);
        }
        return user;
    }

    @GET
    @Path("me")
    public Response getMe() {
        User user = userRepository.getByKeycloakUserId(jwtToken.getClaim("sub"));
        if (user != null) {
            return Response.ok(user).status(200).build();
        } else {
            throw new WebApplicationException("User mapping does not exist.", 404);
        }
    }

    @POST
    @Path("me")
    @Transactional
    public Response createMe(CreateMeRequest request) {
        User user = userRepository.create(new CreateUserRequest(jwtToken.getClaim("sub"), null));
        return Response.ok(user).status(201).build();
    }
}