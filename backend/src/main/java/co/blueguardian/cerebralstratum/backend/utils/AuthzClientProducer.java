package co.blueguardian.cerebralstratum.backend.utils;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;

import java.util.Map;
import java.util.Optional;

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
     * Escape hatch for dev setups where the backend is reached through a tunnel hostname
     * different from Keycloak Dev Services' auto-injected localhost URL (e.g. testing
     * against a mobile device via Cloudflare Tunnel). Keycloak's UMA-ticket token endpoint
     * rejects a bearer token whose `iss` doesn't match however it was just reached, so
     * AuthzClient must call Keycloak via the SAME hostname that minted the caller's token —
     * unset (the default) preserves the existing localhost behaviour.
     */
    @ConfigProperty(name = "cerebral-stratum.authz-server-url")
    Optional<String> authzServerUrlOverride;

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
        String baseAuthServerUrl;
        if (authzServerUrlOverride.isPresent() && !authzServerUrlOverride.get().isBlank()) {
            baseAuthServerUrl = authzServerUrlOverride.get();
        } else {
            String realmSuffix = "/realms/" + realm;
            baseAuthServerUrl = authServerUrl.endsWith(realmSuffix)
                    ? authServerUrl.substring(0, authServerUrl.length() - realmSuffix.length())
                    : authServerUrl;
        }
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
