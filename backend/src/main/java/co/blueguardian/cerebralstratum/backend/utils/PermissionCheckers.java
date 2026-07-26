package co.blueguardian.cerebralstratum.backend.utils;

import io.quarkus.security.PermissionChecker;
import io.quarkus.security.credential.TokenCredential;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;

import java.util.Set;
import java.util.UUID;

@RequestScoped
public class PermissionCheckers {

    private static final Logger LOG = Logger.getLogger(PermissionCheckers.class);

    @Inject
    io.quarkus.security.identity.SecurityIdentity securityIdentity;
    private JsonWebToken getJwt() {
        return (JsonWebToken) securityIdentity.getPrincipal();
    }

    @Inject
    AuthzClient authzClient;

    @ConfigProperty(name = "cerebral-stratum.platform-admin-group", defaultValue = "platform-admins")
    String PlatformAdminsGroupName;

    @ConfigProperty(name = "cerebral-stratum.message-classifier-group", defaultValue = "message-classifier")
    String MessageClassifierGroupName;

    @PermissionChecker("member-of-group")
    public boolean isMemberOfGroup(String group_name) {
        Set<String> groups = getJwt().getClaim("groups");
        LOG.info("Is /" + group_name + " in these groups " + groups + " = " + groups.contains("/" + group_name));
        return groups.contains("/" + group_name);
    }
    @PermissionChecker("device-read")
    public boolean hasDeviceReadAccess(UUID device_uuid) {
        return hasDeviceScope(device_uuid, "device:read");
    }

    @PermissionChecker("device-modify")
    public boolean hasDeviceModifyAccess(UUID device_uuid) {
        return hasDeviceScope(device_uuid, "device:modify");
    }

    /**
     * Backend-mediated UMA check (ADR-0005 Model B): exchange the caller's own bearer
     * token for an RPT scoped to this device resource/scope. A granted RPT is discarded
     * immediately — it is never handed back to the caller, only used as this boolean's
     * verdict. The platform-admins bypass that used to live here is now a Keycloak group
     * policy aggregated into each device's scope-permission (see realm.json).
     */
    private boolean hasDeviceScope(UUID device_uuid, String scope) {
        TokenCredential credential = securityIdentity.getCredential(TokenCredential.class);
        if (credential == null || credential.getToken() == null) {
            return false;
        }
        AuthorizationRequest request = new AuthorizationRequest();
        request.addPermission(device_uuid.toString(), scope);
        try {
            authzClient.authorization(credential.getToken()).authorize(request);
            return true;
        } catch (AuthorizationDeniedException e) {
            LOG.info("Denied " + scope + " on device " + device_uuid + ": " + e.getMessage());
            return false;
        } catch (RuntimeException e) {
            // Fail closed: an unreachable/misbehaving authorization server or an invalid
            // (e.g. test-synthesized, non-Keycloak-issued) token must deny, not 500.
            LOG.warn("Could not evaluate " + scope + " on device " + device_uuid + ": " + e.getMessage());
            return false;
        }
    }
    @PermissionChecker("organisation-admin")
    public boolean isAnOrganisationAdmin(UUID organisation_uuid) {
        Set<String> groups = getJwt().getGroups();
        return (groups.contains(PlatformAdminsGroupName) || groups.contains("/" + organisation_uuid));
    }
    @PermissionChecker("user-admin")
    public boolean isAnUserAdminString(UUID user_uuid) {
        Set<String> groups = getJwt().getGroups();
        UUID user = UUID.fromString(getJwt().getClaim("sub"));
        LOG.info("Checking user: " + user + " against user_uuid: " + user_uuid + " or platform admin: " + PlatformAdminsGroupName);
        return (groups.contains("/" + PlatformAdminsGroupName) || user.equals(user_uuid));
    }
//    @PermissionChecker("message-classifier")
//    public boolean isAMessageClassifier(String device_uuid) {
//        Set<String> groups = jwtToken.getGroups();
//        return groups.contains(MessageClassifierGroupName);
//    }
}
