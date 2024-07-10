package api.v1.bids;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/bids")
@Authenticated
public class BidsResource {
    @Inject
    SecurityIdentity identity;
    private final String username = identity.getPrincipal().getName();

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "All bids view for " + username;
    }

}