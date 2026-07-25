package co.blueguardian.cerebralstratum.backend.controllers.statuses;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class StatusResourceIT {

    private static final String DEVICE_ID = UUID.randomUUID().toString();

    @Test
    void listRequiresAuthentication() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status")
        .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void listAllowedForAdmin() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "regular-user", roles = "user")
    void listForbiddenForNonAdmin() {
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
    @TestSecurity(user = "platform-admin-user", roles = "user")
    @OidcSecurity(claims = @Claim(key = "groups", value = "[\"/platform-admins\"]", type = ClaimType.JSON_ARRAY))
    void getSpecificStatusReturns404WhenMissing() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status/999999")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "platform-admin-user", roles = "user")
    @OidcSecurity(claims = @Claim(key = "groups", value = "[\"/platform-admins\"]", type = ClaimType.JSON_ARRAY))
    void getLatestReturns404WhenNoneExist() {
        given()
        .when()
            .get("/api/v1/devices/by-id/" + DEVICE_ID + "/status/latest")
        .then()
            .statusCode(404);
    }
}
