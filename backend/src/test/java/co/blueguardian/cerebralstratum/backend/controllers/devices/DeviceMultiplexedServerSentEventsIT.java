package co.blueguardian.cerebralstratum.backend.controllers.devices;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

/**
 * Only exercises the pre-authorization request validation: the streaming success path is an
 * open-ended SSE connection (see DevicePermissionIT for why a live Keycloak token would be
 * needed for the authorization check itself), which a synchronous RestAssured request would
 * block on indefinitely, so it isn't asserted here.
 */
@QuarkusTest
class DeviceMultiplexedServerSentEventsIT {

    @Test
    @TestSecurity(user = "some-user")
    void locationsRequiresAtLeastOneDevice() {
        given()
        .when()
            .get("/api/v1/devices/locations")
        .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "some-user")
    void statusesRequiresAtLeastOneDevice() {
        given()
        .when()
            .get("/api/v1/devices/statuses")
        .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "some-user")
    void canbusesRequiresAtLeastOneDevice() {
        given()
        .when()
            .get("/api/v1/devices/canbuses")
        .then()
            .statusCode(400);
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        given()
            .queryParam("devices", "d4197c07-cf51-4e4d-b1aa-4c5d6e7f8091")
        .when()
            .get("/api/v1/devices/locations")
        .then()
            .statusCode(401);
    }
}
