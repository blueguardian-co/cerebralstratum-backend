package co.blueguardian.cerebralstratum.backend.controllers.statuses;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class StatusResourceIT {

    private static final String DEVICE_ID = UUID.randomUUID().toString();

    /**
     * device-read is now a UMA-ticket check (ADR-0005) evaluated by a real Dev Services
     * Keycloak, so @TestSecurity/@OidcSecurity's synthetic (non-Keycloak-issued) identity can
     * no longer stand in for it — nor can a made-up device UUID, since it isn't a real UMA
     * resource. This uses a real password-grant token against the dedicated IT device/user
     * fixtures in devservices/realm.json instead. See DevicePermissionIT for the permission
     * checks themselves; these two just confirm the 404-when-no-data path still works once
     * permission is granted for real.
     */
    private static final String UMA_DEVICE_ID = "d4197c07-cf51-4e4d-b1aa-4c5d6e7f8091";

    private static String ownerToken() {
        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "cerebral-stratum-frontend")
                .formParam("username", "device-it-owner@example.com")
                .formParam("password", "device-it-owner-pw")
                .when()
                .post("http://localhost:8180/realms/external/protocol/openid-connect/token")
                .then()
                .statusCode(200)
                .extract().path("access_token");
    }

    @Test
    void listRequiresAuthentication() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status")
        .then()
            .statusCode(401);
    }

    /**
     * getAllStatuses moved from @RolesAllowed("admins") to @PermissionsAllowed("device-read")
     * (zero-standing-operator-access correction, ADR-0005) — the device owner, not a platform
     * admin role, is what grants access here now.
     */
    @Test
    void listAllowedForOwner() {
        given()
            .auth().oauth2(ownerToken())
        .when()
            .get("/api/v1/devices/by-id/" + UMA_DEVICE_ID + "/status")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "regular-user", roles = "user")
    void listForbiddenWithoutRealGrant() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status")
        .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "regular-user", roles = "user")
    void getSpecificStatusForbiddenWithoutDeviceGroupClaim() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status/999999")
        .then()
            .statusCode(403);
    }

    @Test
    void getSpecificStatusReturns404WhenMissing() {
        given()
            .auth().oauth2(ownerToken())
        .when()
            .get("/api/v1/devices/by-id/" + UMA_DEVICE_ID + "/status/999999")
        .then()
            .statusCode(404);
    }

    @Test
    void getLatestReturns404WhenNoneExist() {
        given()
            .auth().oauth2(ownerToken())
        .when()
            .get("/api/v1/devices/by-id/" + UMA_DEVICE_ID + "/status/latest")
        .then()
            .statusCode(404);
    }
}
