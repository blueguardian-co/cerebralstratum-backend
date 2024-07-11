package api.v1.auctions;

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

@Path("/api/v1/auctions")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuctionsResource {

    @Inject
    EntityManager entityManager;

    @GET
    public List<Auctions> getAllAuctions() {
        return entityManager.createNamedQuery("Auctions.findAll", Auctions.class)
            .getResultList();          
    }

    @POST
    @Transactional
    public Response create(Auctions auction) {
        if (auction.getId() != null) {
            throw new WebApplicationException("ID was invalidly set on request.", 422);
        }

        entityManager.persist(auction);
        return Response.ok(auction).status(201).build();
    }

    @GET
    @Path("{id}")
    public Auctions getSpecificAuction(Integer id) {
        Auctions entity = entityManager.find(Auctions.class, id);
        if (entity == null) {
            throw new WebApplicationException("Auction with id of " + id + " does not exist.", 404);
        }
        return entity;
    }
}