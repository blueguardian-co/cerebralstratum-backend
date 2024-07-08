package org.acme.security.keycloak.authorization;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.resteasy.reactive.NoCache;

import io.quarkus.security.identity.SecurityIdentity;

@Path("/api/v1/bids")
@Authenticated
@Inject
public class BidsResource {
    SecurityIdentity identity;
    private final String username = identity.getPrincipal().getName();
}