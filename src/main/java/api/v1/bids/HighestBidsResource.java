package api.v1.bids;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;

import io.quarkus.security.Authenticated;

@Path("/api/v1/bids/highest")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HighestBidsResource {

    @Inject
    EntityManager entityManager;

    @GET
    public List<Bids> getAllBids() {
        return entityManager.createNamedQuery("HighestBids.findAll", Bids.class)
            .getResultList();          
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
}