package controllers.bids;

import repositories.bids.BidRepository;

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

@Path("/api/v1/auction/{auction_id}/bids")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BidsResource {

    @Inject
    SecurityIdentity securityIdentity;
    @Inject
    BidRepository bidRepository;


    @GET
    public List<Bid> getAllBids() {
        return bidRepository.findAll();          
    }

    @POST
    @Transactional
    @RolesAllowed("bidder")
    public Response create(CreateBidRequest request) {
        Bid bid = bidRepository.create(request);
        return Response.ok(bid).status(201).build();
    }

    @DELETE
    @RolesAllowed({"admin"})
    @Transactional
    public Response delete(DeleteBidRequest request) {
        Bid bid = bidRepository.delete(request);
        return Response.ok(bid).status(201).build();
    }

    @GET
    @Path("{id}")
    public Bid getBid(Integer id) {
        Bid bid = bidRepository.getById(id);
        if (bid == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return bid;
    }

    @GET
    @Path("user/{user_id}")
    @RolesAllowed("bidder")
    public List<Bid> getBidsByUserAndAuction(Integer auction_id, Integer user_id) {
        return bidRepository.getByUserAndAuction(auction_id, user_id);
    }

    // @SuppressWarnings("unchecked")
    // @GET
    // @Path("api/v1/auctions/{id}/bids/highest")
    // public List<Bids> getAllHighestBids() {
    //     String sql = "SELECT DISTINCT ON (b.auction_id) b.id, b.auction_id, b.user_id, u.username, u.table_number, b.bid_time, b.bid_amount FROM bids b INNER JOIN user_info u on b.user_id = u.id ORDER BY b.auction_id ASC, b.bid_amount DESC, b.bid_time ASC;";
    //     return entityManager.createNativeQuery(sql, Bids.class).getResultList();      
    // }
}
