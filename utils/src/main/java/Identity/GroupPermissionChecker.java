package Identity;

import io.quarkus.security.PermissionChecker;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@ApplicationScoped
public class GroupPermissionChecker {
    @Inject
    JsonWebToken jwtToken;

    @PermissionChecker("member-of-group")
    public boolean isMemberOfGroup(String group_name) {
        Set<String> groups = jwtToken.getGroups();
        return groups.contains(group_name);
    }
}
