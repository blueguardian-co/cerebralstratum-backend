package api.v1.user;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/users")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    EntityManager entityManager;
    @Inject
    SecurityIdentity identity;

    @GET
    public List<User> getAllUser() {
        return entityManager.createNamedQuery("User.findAll", User.class)
            .getResultList();          
    }

    @POST
    @Transactional
    public Response create(User user) {
        if (user.getId() != null) {
            throw new WebApplicationException("ID was invalidly set on request.", 422);
        }

        entityManager.persist(user);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("{id}")
    public User getSpecificAuction(Integer id) {
        User entity = entityManager.find(User.class, id);
        if (entity == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    // @GET
    // @Path("/me")
    // public User me() {
    //     /*
    //     Return user details and table number
    //     */
    //     return new User(identity);
    // }
    // @POST
    // @Path("/me")
    // @NoCache
    // public User update(String user, int tableNumber) {
    //     /*
    //     This endpoint should be used to update the user's table number
    //     */
    //     return new User(identity);
    // private final String username;

    // User(SecurityIdentity identity) {
    //     this.username = identity.getPrincipal().getName();
    // }

    // public String getUserName() {
    //     return username;
    // }
    // }
}