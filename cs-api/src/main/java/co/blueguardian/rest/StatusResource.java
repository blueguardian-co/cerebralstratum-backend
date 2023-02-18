package co.blueguardian.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/api/v1/healthz")
public class StatusResource {

    private Status status = new Status();
    public StatusResource() {
        status = new Status("ok");
    }

    @GET
    public Status returnStatus() {
        return status;
    }
}