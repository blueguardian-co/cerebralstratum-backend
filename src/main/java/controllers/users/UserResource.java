package controllers.users;

import repositories.users.UserRepository;

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

@Path("/api/v1")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    UserRepository userRepository;

    @GET
    @Path("users")
    @RolesAllowed("admin")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @POST
    @Path("users")
    @Transactional
    @RolesAllowed("admin")
    public Response create(CreateUserRequest request) {
        User user = userRepository.create(request);
        return Response.ok(user).status(201).build();
    }

    @DELETE
    @Path("users")
    @Transactional
    @RolesAllowed("admin")
    public Response delete(DeleteUserRequest request) {
        User user = userRepository.delete(request);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("users/{user_id}")
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
        try {
            User user = userRepository.getByUsername(securityIdentity.getPrincipal().getName());
            return Response.ok(user).status(201).build();
        } catch (Exception e) {
            throw new WebApplicationException("User mapping does not exist.", 404);
        }
    }

    @POST
    @Path("me")
    @Transactional
    public Response createMe(CreateMeRequest request) {
        User user = userRepository.create(
                new CreateUserRequest(
                        securityIdentity.getPrincipal().getName(),
                        request.table_number));
        return Response.ok(user).status(201).build();
    }
}