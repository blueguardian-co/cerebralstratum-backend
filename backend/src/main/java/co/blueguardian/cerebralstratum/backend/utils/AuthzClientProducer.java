package co.blueguardian.cerebralstratum.backend.utils;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.util.Map;

/**
 * quarkus-keycloak-authorization only produces its own AuthzClient bean when
 * quarkus.keycloak.policy-enforcer.enabled=true (true in %prod, false in %dev/%test here —
 * deliberately, since the automatic per-path Model A enforcer would conflict with the
 * backend-mediated Model B device checks in PermissionCheckers, per ADR-0005). @DefaultBean
 * makes this producer a fallback: Quarkus's own bean wins in %prod, this one fills the gap
 * everywhere else, so AuthzClient is always injectable regardless of profile.
 */
@ApplicationScoped
public class AuthzClientProducer {

    @ConfigProperty(name = "quarkus.oidc.auth-server-url", defaultValue = "")
    String authServerUrl;

    @ConfigProperty(name = "keycloak.realm", defaultValue = "")
    String realm;

    @ConfigProperty(name = "quarkus.oidc.client-id", defaultValue = "")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret", defaultValue = "")
    String clientSecret;

    /**
     * quarkus.oidc.auth-server-url is the full per-realm URL (Quarkus OIDC convention),
     * but AuthzClient's Configuration wants the bare server URL and appends
     * /realms/{realm}/.well-known/uma2-configuration itself (native Keycloak adapter
     * convention) — strip the realm suffix before handing it over.
     */
    @Produces
    @DefaultBean
    @ApplicationScoped
    public AuthzClient authzClient() {
        String realmSuffix = "/realms/" + realm;
        String baseAuthServerUrl = authServerUrl.endsWith(realmSuffix)
                ? authServerUrl.substring(0, authServerUrl.length() - realmSuffix.length())
                : authServerUrl;
        Configuration configuration = new Configuration(
                baseAuthServerUrl,
                realm,
                clientId,
                Map.of("secret", clientSecret),
                null
        );
        return AuthzClient.create(configuration);
    }
}
