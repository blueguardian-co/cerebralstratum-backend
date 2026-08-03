package co.blueguardian.cerebralstratum.backend.controllers.devices;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;

/**
 * Exercises the UMA-ticket-backed device-read/device-modify checkers (ADR-0005) against a
 * real Dev Services Keycloak using real access tokens for seeded users — @TestSecurity's
 * synthetic identity never hits Keycloak, so it can't be validated by AuthzClient.authorize(),
 * which checks a real RPT against real Keycloak-issued signing keys. See devservices/realm.json
 * for the "d4197c07-cf51-4e4d-b1aa-4c5d6e7f8091" test device and its three dedicated users.
 *
 * Targets StatusResource/LocationResource rather than DevicesResource.getDeviceById/updateDevice:
 * those two query the `devices` table directly, which requires a seeded Postgres row — but
 * %test's Liquibase changelog (db/changeLog.yaml) doesn't include the devservices-only test
 * data fixtures (those are %dev-only, for manual testing). Status/location endpoints only
 * touch their own child tables, so — like StatusResourceIT — they work against this device
 * purely from the Keycloak-side fixtures, with no Postgres device row required.
 */
@QuarkusTest
class DevicePermissionIT {

    private static final String DEVICE_ID = "d4197c07-cf51-4e4d-b1aa-4c5d6e7f8091";
    private static final String KEYCLOAK_TOKEN_URL = "http://localhost:8180/realms/external/protocol/openid-connect/token";

    private static String tokenFor(String username, String password) {
        return given()
                .contentType("application/x-www-form-urlencoded")
                .formParam("grant_type", "password")
                .formParam("client_id", "cerebral-stratum-frontend")
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post(KEYCLOAK_TOKEN_URL)
                .then()
                .statusCode(200)
                .extract().path("access_token");
    }

    private static String ownerToken() {
        return tokenFor("device-it-owner@example.com", "device-it-owner-pw");
    }

    private static String strangerToken() {
        return tokenFor("device-it-stranger@example.com", "device-it-stranger-pw");
    }

    private static String platformAdminToken() {
        return tokenFor("device-it-platform-admin@example.com", "device-it-platform-admin-pw");
    }

    @Test
    void ownerHasReadAccess() {
        given()
            .auth().oauth2(ownerToken())
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/statuses/latest")
        .then()
            .statusCode(404);
    }

    /**
     * Zero-standing-operator-access posture (ADR-0005 amendment): platform-admins get no
     * implicit device:read/device:modify grant. A platform admin with no other relationship
     * to this device is denied exactly like a stranger.
     */
    @Test
    void platformAdminHasNoStandingReadAccess() {
        given()
            .auth().oauth2(platformAdminToken())
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/statuses/latest")
        .then()
            .statusCode(403);
    }

    @Test
    void strangerIsDeniedReadAccess() {
        given()
            .auth().oauth2(strangerToken())
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/statuses/latest")
        .then()
            .statusCode(403);
    }

    /**
     * LocationResource.delete has no path segment for location_id (a pre-existing quirk,
     * unrelated to this migration) so the repository call underneath receives a null id and
     * 500s once permission is granted — not something worth fixing as a drive-by here. The
     * permission layer itself is what's under test: a granted (non-owner-denied) request
     * reaches that 500, a denied one never gets past the checker, so "not 403" is the
     * correct, robust signal for "permission was granted" independent of that downstream bug.
     */
    @Test
    void ownerHasModifyAccess() {
        given()
            .auth().oauth2(ownerToken())
        .when()
            .delete("/api/v1/devices/by-id/" + DEVICE_ID + "/locations")
        .then()
            .statusCode(not(403));
    }

    @Test
    void strangerIsDeniedModifyAccess() {
        given()
            .auth().oauth2(strangerToken())
        .when()
            .delete("/api/v1/devices/by-id/" + DEVICE_ID + "/locations")
        .then()
            .statusCode(403);
    }
}
