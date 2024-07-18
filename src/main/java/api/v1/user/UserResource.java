package api.v1.user;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
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

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    EntityManager entityManager;
    @Inject
    SecurityIdentity securityIdentity;

    @GET
    @Path("users")
    @RolesAllowed({"admin"})
    public List<User> getAllUser() {
        return entityManager.createNamedQuery("User.findAll", User.class)
            .getResultList();          
    }

    @POST
    @Path("users")
    @Transactional
    @RolesAllowed({"admin"})
    public Response create(User user) {
        if (user.getId() != null) {
            throw new WebApplicationException("ID was invalidly set on request.", 422);
        }

        entityManager.persist(user);
        return Response.ok(user).status(201).build();
    }

    @GET
    @Path("users/{id}")
    @RolesAllowed({"admin"})
    public User getUser(Integer id) {
        User entity = entityManager.find(User.class, id);
        if (entity == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @GET
    @Path("me")
    @RolesAllowed({"bidder"})
    public User getMe() {
        User user = entityManager.createNamedQuery("User.getUser", User.class)
            .setParameter("username", securityIdentity.getPrincipal().getName())
            .getSingleResult();
        if (user == null) {
            throw new WebApplicationException("User mapping does not exist.", 404);
        }
        return user;
    }
}