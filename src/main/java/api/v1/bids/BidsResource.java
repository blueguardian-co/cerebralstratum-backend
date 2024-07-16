package api.v1.bids;

import api.v1.user.User;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.RolesAllowed;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/bids")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BidsResource {

    @Inject
    EntityManager entityManager;
    @Inject
    SecurityIdentity securityIdentity;


    @GET
    public List<Bids> getAllBids() {
        return entityManager.createNamedQuery("Bids.findAll", Bids.class)
            .getResultList();          
    }

    @POST
    @Transactional
    @RolesAllowed("bidder")
    public Response create(Bids bids) {

        if (bids.getId() != null) {
            throw new WebApplicationException("ID was invalidly set on request.", 422);
        }

        List<User> oidcUserIdList = entityManager.createNamedQuery("User.getUser", User.class)
            .setParameter("username", securityIdentity.getPrincipal().getName())
            .getResultList();

        // Get requesting user's ID from the user_info table
        int oidcUserId = oidcUserIdList.get(0).getId();
        // Get requesting user's ID from the username from the IdP, and lookup in user_info
        int requestingUserId = bids.getUser().getId();

        // Compare that the two IDs are the same
        if (oidcUserId != requestingUserId) {
            throw new WebApplicationException("You can only place bids on your own behalf.", 403);
        }

        entityManager.persist(bids);
        return Response.ok(bids).status(201).build();
    }

    @GET
    @Path("{id}")
    public Bids getSpecificBid(Integer id) {
        Bids entity = entityManager.find(Bids.class, id);
        if (entity == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @PUT
    @Transactional
    @RolesAllowed("bidder")
    public Response update(Bids bid) {
        if (bid.getId() == null) {
            throw new WebApplicationException("No bid ID was provided.", 422);
        }

        List<User> oidcUserIdList = entityManager.createNamedQuery("User.getUser", User.class)
            .setParameter("username", securityIdentity.getPrincipal().getName())
            .getResultList();

        // Get requesting user's ID from the user_info table
        int oidcUserId = oidcUserIdList.get(0).getId();

        // Get the requested bid user's ID
        int bidUserId = bid.getUser().getId();

        if (oidcUserId != bidUserId) {
            throw new WebApplicationException("You can only update bids on your own behalf.", 403);
        }

        entityManager.merge(bid);
        return Response.ok(bid).status(201).build();
    }

    @GET
    @Path("user/{id}")
    @RolesAllowed("bidder")
    public List<Bids> getBidsByUser(Integer id) {
        return entityManager.createNamedQuery("Bids.byUser", Bids.class)
            .setParameter("userId", id)
            .getResultList();          
    }

    @SuppressWarnings("unchecked")
    @GET
    @Path("highest")
    public List<Bids> getAllHighestBids() {
        String sql = "SELECT DISTINCT ON (b.auction_id) b.id, b.auction_id, b.user_id, u.username, u.table_number, b.bid_time, b.bid_amount FROM bids b INNER JOIN user_info u on b.user_id = u.id ORDER BY b.auction_id ASC, b.bid_amount DESC, b.bid_time ASC;";
        return entityManager.createNativeQuery(sql, Bids.class).getResultList();      
    }
}