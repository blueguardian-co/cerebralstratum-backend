package co.blueguardian.cerebralstratum.backend.repositories.devices;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;
import org.keycloak.representations.idm.authorization.PolicyRepresentation;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.keycloak.representations.idm.authorization.ScopePermissionRepresentation;
import org.keycloak.representations.idm.authorization.ScopeRepresentation;
import org.keycloak.representations.idm.authorization.UserPolicyRepresentation;

import java.util.Set;
import java.util.UUID;

/**
 * Provisions the Keycloak UMA 2.0 resources/policies/permissions that back device-level
 * RBAC (ADR-0005), mirroring what the two seeded fixture devices get by hand in realm.json.
 * Owner access only — no standing platform-admin/operator grant is attached (BlueGuardian's
 * zero-standing-operator-access posture; see ADR-0005's amendment). Non-owner access to a
 * device is always a separate, time-boxed device:share consent grant (ADR-0006/0009), never
 * a policy applied here.
 */
@ApplicationScoped
public class DeviceAuthorizationProvisioningService {

    private static final String DEVICE_RESOURCE_TYPE = "urn:device-fleet:resources:device";

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm")
    String realmName;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String deviceFleetClientId;

    private AuthorizationResource authorization() {
        String internalClientId = keycloak.realm(realmName).clients()
                .findByClientId(deviceFleetClientId).get(0).getId();
        ClientResource client = keycloak.realm(realmName).clients().get(internalClientId);
        return client.authorization();
    }

    /** create() phase 1 (ADR-0001): the resource exists but has no owner yet. */
    public String createUnownedResource(UUID deviceUuid) {
        ResourceRepresentation resource = new ResourceRepresentation();
        // id == name == device UUID: AuthorizationRequest.addPermission (PermissionCheckers)
        // resolves resources by id, not name, so the two must match for the runtime check to work.
        resource.setId(deviceUuid.toString());
        resource.setName(deviceUuid.toString());
        resource.setType(DEVICE_RESOURCE_TYPE);
        resource.setOwnerManagedAccess(true);
        resource.setScopes(Set.of(new ScopeRepresentation("device:read"), new ScopeRepresentation("device:modify")));
        try (Response response = authorization().resources().create(resource)) {
            return response.readEntity(ResourceRepresentation.class).getId();
        }
    }

    /** registerDevice() phase 2 (ADR-0001): set the owner and grant them device:read+device:modify. */
    public void grantOwnerAccess(UUID deviceUuid, String keycloakResourceId, UUID ownerUserId) {
        AuthorizationResource authz = authorization();

        ResourceRepresentation resource = authz.resources().resource(keycloakResourceId).toRepresentation();
        resource.setOwner(ownerUserId.toString());
        authz.resources().resource(keycloakResourceId).update(resource);

        String ownerPolicyName = ownerPolicyName(deviceUuid);
        UserPolicyRepresentation ownerPolicy = new UserPolicyRepresentation();
        ownerPolicy.setName(ownerPolicyName);
        ownerPolicy.setLogic(Logic.POSITIVE);
        ownerPolicy.addUser(ownerUserId.toString());
        authz.policies().user().create(ownerPolicy).close();

        ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
        permission.setName(accessPermissionName(deviceUuid));
        permission.setDecisionStrategy(DecisionStrategy.AFFIRMATIVE);
        permission.addResource(deviceUuid.toString());
        permission.addScope("device:read", "device:modify");
        permission.addPolicy(ownerPolicyName);
        authz.permissions().scope().create(permission).close();
    }

    /** unregisterDevice(): reverse grantOwnerAccess, leaving the resource itself intact and unowned. */
    public void revokeOwnerAccess(UUID deviceUuid) {
        AuthorizationResource authz = authorization();

        ScopePermissionRepresentation permission = authz.permissions().scope().findByName(accessPermissionName(deviceUuid));
        if (permission != null) {
            authz.permissions().scope().findById(permission.getId()).remove();
        }

        PolicyRepresentation ownerPolicy = authz.policies().findByName(ownerPolicyName(deviceUuid));
        if (ownerPolicy != null) {
            authz.policies().policy(ownerPolicy.getId()).remove();
        }
    }

    /** deleteDevice(): remove the Keycloak resource entirely. Safe to call even if never owned. */
    public void deleteResource(String keycloakResourceId) {
        if (keycloakResourceId == null) {
            return;
        }
        authorization().resources().resource(keycloakResourceId).remove();
    }

    private static String ownerPolicyName(UUID deviceUuid) {
        return deviceUuid + "-owner-policy";
    }

    private static String accessPermissionName(UUID deviceUuid) {
        return deviceUuid + "-access-permission";
    }
}
