package co.blueguardian.cerebralstratum.utils.identity;

import io.quarkus.security.PermissionChecker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@ApplicationScoped
public class GroupPermissionChecker {
    @Inject
    JsonWebToken jwtToken;

    @ConfigProperty(name = "cerebral-stratum.platform-admin-group", defaultValue = "admins")
    String PlatformAdminsGroupName;

    @ConfigProperty(name = "cerebral-stratum.message-classifier-group", defaultValue = "message-classifier")
    String MessageClassifierGroupName;

    @PermissionChecker("member-of-group")
    public boolean isMemberOfGroup(String group_name) {
        Set<String> groups = jwtToken.getGroups();
        return groups.contains(group_name);
    }
    @PermissionChecker("platform-admin")
    public boolean isAPlatformAdmin(String device_uuid) {
        Set<String> groups = jwtToken.getGroups();
        return groups.contains(PlatformAdminsGroupName);
    }
    @PermissionChecker("message-classifier")
    public boolean isAMessageClassifier(String device_uuid) {
        Set<String> groups = jwtToken.getGroups();
        return groups.contains(MessageClassifierGroupName);
    }
}
