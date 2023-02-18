package co.blueguardian.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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