package controllers.auctions;

import repositories.auctions.AuctionRepository;

import java.util.List;

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
import io.quarkus.security.Authenticated;

@Path("/api/v1/auctions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuctionsResource {

    @Inject
    AuctionRepository auctionRepository;

    @GET
    public List<Auction> getAllAuctions() {
        return auctionRepository.findAll();
    }

    @POST
    @RolesAllowed({"admin"})
    @Transactional
    public Response create(CreateAuctionRequest request) {
        Auction auction = auctionRepository.create(request);
        return Response.ok(auction).status(201).build();
    }

    @DELETE
    @RolesAllowed({"admin"})
    @Transactional
    public Response delete(DeleteAuctionRequest request) {
        Auction auction = auctionRepository.delete(request);
        return Response.ok(auction).status(201).build();
    }

    @GET
    @Path("{id}")
    public Auction getAuction(Integer id) {
        Auction auction = auctionRepository.getById(id);
        if (auction == null) {
            throw new WebApplicationException("Auction with id of " + id + " does not exist.", 404);
        }
        return auction;
    }
}