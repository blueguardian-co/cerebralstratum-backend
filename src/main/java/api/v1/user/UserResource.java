package api.v1.user;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/users")
@Authenticated
public class UserResource {

    @Inject
    SecurityIdentity identity;

    @GET
    @Path("/me")
    @NoCache
    public User me() {
        /*
        Return user details and table number
        */
        return new User(identity);
    }

    // @POST
    // @Path("/me")
    // @NoCache
    // public User update(String user, int tableNumber) {
    //     /*
    //     This endpoint should be used to update the user's table number
    //     */
    //     return new User(identity);
    // }

    public static class User {

        private final String username;

        User(SecurityIdentity identity) {
            this.username = identity.getPrincipal().getName();
        }

        public String getUserName() {
            return username;
        }
    }
}