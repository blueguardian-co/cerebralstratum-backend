package api.v1.bids;

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

import io.quarkus.security.Authenticated;

@Path("/api/v1/bids")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BidsResource {

    @Inject
    EntityManager entityManager;

    @GET
    public List<Bids> getAllBids() {
        return entityManager.createNamedQuery("Bids.findAll", Bids.class)
            .getResultList();          
    }

    @POST
    @Transactional
    public Response create(Bids bids) {
        if (bids.getId() != null) {
            throw new WebApplicationException("ID was invalidly set on request.", 422);
        }

        entityManager.persist(bids);
        return Response.ok(bids).status(201).build();
    }

    @GET
    @Path("{id}")
    public Bids getSpecificAuction(Integer id) {
        Bids entity = entityManager.find(Bids.class, id);
        if (entity == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return entity;
    }

    @GET
    @Path("highest")
    public List<Bids> getAllHighestBids() {
        return entityManager.createNamedQuery("HighestBids.findAll", Bids.class)
            .getResultList();          
    }

    @GET
    @Path("highest/{id}")
    public Bids getSpecificHighestBid(Integer id) {
        Bids entity = entityManager.find(Bids.class, id);
        if (entity == null) {
            throw new WebApplicationException("Bid with id of " + id + " does not exist.", 404);
        }
        return entity;
    }
}