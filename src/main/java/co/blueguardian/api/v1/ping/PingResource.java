package co.blueguardian.api.v1.ping;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/v1/ping")
public class PingResource {

    private Ping ping = new Ping();
    public PingResource() {
        ping = new Ping("pong");
    }

    @GET
    public Ping returnPingStatus() {
        return ping;
    }
}