package co.blueguardian.cerebralstratum.backend.utils;

import io.quarkus.security.PermissionChecker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class PermissionCheckers {

    private static final Logger LOG = Logger.getLogger(PermissionCheckers.class);

    @Inject
    JsonWebToken jwtToken;

    @ConfigProperty(name = "cerebral-stratum.platform-admin-group", defaultValue = "platform-admins")
    String PlatformAdminsGroupName;

    @ConfigProperty(name = "cerebral-stratum.message-classifier-group", defaultValue = "message-classifier")
    String MessageClassifierGroupName;

    @PermissionChecker("member-of-group")
    public boolean isMemberOfGroup(String group_name) {
        Set<String> groups = jwtToken.getClaim("groups");
        LOG.info("Is /" + group_name + " in these groups " + groups + " = " + groups.contains("/" + group_name));
        return groups.contains("/" + group_name);
    }
    @PermissionChecker("member-of-device-group")
    public boolean isMemberOfDeviceGroup(String device_uuid) {
        Set<String> groups = jwtToken.getGroups();
        LOG.info("Is /" + device_uuid + " in these groups " + groups);
        return (groups.contains("/" + device_uuid) || groups.contains("/" + device_uuid + "/view-only") || groups.contains("/" + device_uuid + "/modify"));
    }

    @PermissionChecker("device-admin")
    public boolean isADeviceAdmin(String device_uuid) {
        Set<String> groups = jwtToken.getGroups();
        return (groups.contains(PlatformAdminsGroupName) || groups.contains(device_uuid));
    }
    @PermissionChecker("organisation-admin")
    public boolean isAnOrganisationAdmin(String organisation_uuid) {
        Set<String> groups = jwtToken.getGroups();
        return (groups.contains(PlatformAdminsGroupName) || groups.contains(organisation_uuid));
    }
    @PermissionChecker("user-admin")
    public boolean isAnUserAdminString(UUID user_uuid) {
        Set<String> groups = jwtToken.getGroups();
        UUID user = UUID.fromString(jwtToken.getClaim("sub"));
        return (groups.contains(PlatformAdminsGroupName) || user.equals(user_uuid));
    }
//    @PermissionChecker("message-classifier")
//    public boolean isAMessageClassifier(String device_uuid) {
//        Set<String> groups = jwtToken.getGroups();
//        return groups.contains(MessageClassifierGroupName);
//    }
}
