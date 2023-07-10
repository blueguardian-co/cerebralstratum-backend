package co.blueguardian.api.v1;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/v1/healthstatus")
public class HealthStatusResource {

    private HealthStatus status = new HealthStatus();
    public HealthStatusResource() {
        status = new HealthStatus() {{
            overall="OK";
            backend="OK";
        }};
    }

    @GET
    public HealthStatus returnHealthStatus() {
        return status;
    }
}