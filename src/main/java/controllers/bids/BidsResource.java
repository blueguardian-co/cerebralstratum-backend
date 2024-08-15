package controllers.bids;

import repositories.auctions.AuctionRepository;
import controllers.auctions.Auction;
import repositories.bids.BidRepository;


import java.time.LocalDateTime;
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

@Path("/api/v1/auctions/{auction_id}/bids")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BidsResource {

    @Inject
    SecurityIdentity securityIdentity;
    @Inject
    BidRepository bidRepository;
    @Inject
    AuctionRepository auctionRepository;

    @GET
    public List<Bid> getAllBids(Integer auction_id) {
        return bidRepository.findAll(auction_id);
    }

    @POST
    @Transactional
    public Response create(Integer auction_id, CreateBidRequest request) {
        String username = securityIdentity.getPrincipal().getName();
        LocalDateTime now = LocalDateTime.now();
        Auction auction = auctionRepository.getById(auction_id);
        if ((now.isAfter(auction.auction_start) || now.isEqual(auction.auction_start)) && now.isBefore(auction.auction_end)) {
            Bid bid = bidRepository.create(auction_id, username, request);
            return Response.ok(bid).status(201).build();
        } else {
            throw new WebApplicationException("Auction: " + auction.item_name + " is closed. Bid rejected.", 406);
        }
    }

    @DELETE
    @RolesAllowed("admin")
    @Transactional
    public Response delete(Integer auction_id, Integer bid_id) {
        Bid bid = bidRepository.delete(bid_id);
        return Response.ok(bid).status(201).build();
    }

    @GET
    @Path("{bid_id}")
    public Bid getBid(Integer auction_id, Integer bid_id) {
        Bid bid = bidRepository.getById(bid_id);
        if (bid == null) {
            throw new WebApplicationException("Bid with id of " + bid_id + " does not exist.", 404);
        }
        return bid;
    }

    @GET
    @Path("user/{user_id}")
    @RolesAllowed({ "bidder" })
    public List<Bid> getBidsByUserAndAuction(Integer auction_id, Integer user_id) {
        return bidRepository.getByUserAndAuction(auction_id, user_id);
    }

    @GET
    @Path("highest")
    public Bid getAllHighestBids(Integer auction_id) {
        try {
            return bidRepository.getHighestByAuction(auction_id);
        } catch (Exception e) {
            throw new WebApplicationException("No bids exist on auction.", 404);
        }
    }
}
