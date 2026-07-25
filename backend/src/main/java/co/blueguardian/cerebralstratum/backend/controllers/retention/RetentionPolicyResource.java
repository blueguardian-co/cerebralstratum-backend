package co.blueguardian.cerebralstratum.backend.controllers.retention;

import co.blueguardian.cerebralstratum.utils.model.RetentionPolicy;
import co.blueguardian.cerebralstratum.utils.model.RetentionSubjectType;
import co.blueguardian.cerebralstratum.backend.repositories.locations.LocationRepository;
import co.blueguardian.cerebralstratum.backend.repositories.retention.RetentionPolicyRepository;
import co.blueguardian.cerebralstratum.backend.repositories.statuses.StatusRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.WebApplicationException;

import io.quarkus.security.identity.SecurityIdentity;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/v1/retention-policies")
@RolesAllowed("admins")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RetentionPolicyResource {

    @Inject
    RetentionPolicyRepository retentionPolicyRepository;

    @Inject
    LocationRepository locationRepository;

    @Inject
    StatusRepository statusRepository;

    @Inject
    SecurityIdentity securityIdentity;

    private JsonWebToken getJwt() {
        return (JsonWebToken) securityIdentity.getPrincipal();
    }

    @GET
    public List<RetentionPolicy> getAll(@QueryParam("subject_type") RetentionSubjectType subject_type) {
        return retentionPolicyRepository.findAll(subject_type);
    }

    @GET
    @Path("{id}")
    public RetentionPolicy getById(Integer id) {
        RetentionPolicy policy = retentionPolicyRepository.getById(id);
        if (policy == null) {
            throw new WebApplicationException("Retention policy with id of " + id + " does not exist.", 404);
        }
        return policy;
    }

    @POST
    @Transactional
    public RetentionPolicy upsert(UpsertRetentionPolicyRequest request) {
        UUID updated_by = UUID.fromString(getJwt().getClaim("sub"));
        return retentionPolicyRepository.upsert(request.subject_type, request.subject_id, request.retention_days, "MANUAL", updated_by);
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public RetentionPolicy delete(Integer id) {
        RetentionPolicy policy = retentionPolicyRepository.delete(id);
        if (policy == null) {
            throw new WebApplicationException("Retention policy with id of " + id + " does not exist.", 404);
        }
        return policy;
    }

    // Manual/ops trigger only — the production purge mechanism is the pg_cron job
    // defined in db/changeLogs/1.0.0.yaml, which runs independently of this app.
    @POST
    @Path("purge")
    public Map<String, Integer> purge(@QueryParam("subject_type") RetentionSubjectType subject_type) {
        if (subject_type == RetentionSubjectType.LOCATION) {
            return Map.of("deleted", locationRepository.purgeExpired());
        }
        if (subject_type == RetentionSubjectType.STATUS) {
            return Map.of("deleted", statusRepository.purgeExpired());
        }
        throw new WebApplicationException("No purge implementation for subject type " + subject_type, 400);
    }
}
