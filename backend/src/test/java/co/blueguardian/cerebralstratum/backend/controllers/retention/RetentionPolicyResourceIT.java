package co.blueguardian.cerebralstratum.backend.controllers.retention;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class RetentionPolicyResourceIT {

    private static final String ADMIN_SUB = "11111111-1111-1111-1111-111111111111";

    @Test
    void listRequiresAuthentication() {
        given()
            .queryParam("subject_type", "LOCATION")
        .when()
            .get("/api/v1/retention-policies")
        .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "regular-user", roles = "user")
    void listForbiddenForNonAdmin() {
        given()
            .queryParam("subject_type", "LOCATION")
        .when()
            .get("/api/v1/retention-policies")
        .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void listAllowedForAdmin() {
        given()
            .queryParam("subject_type", "LOCATION")
        .when()
            .get("/api/v1/retention-policies")
        .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    @OidcSecurity(claims = @Claim(key = "sub", value = ADMIN_SUB))
    void upsertInsertsThenUpdatesSameRow() {
        String subjectId = "22222222-2222-2222-2222-222222222222";

        int id = given()
            .contentType("application/json")
            .body("{\"subject_type\":\"STATUS\",\"subject_id\":\"" + subjectId + "\",\"retention_days\":30}")
        .when()
            .post("/api/v1/retention-policies")
        .then()
            .statusCode(200)
            .body("retention_days", equalTo(30))
            .body("source", equalTo("MANUAL"))
            .body("updated_by", equalTo(ADMIN_SUB))
            .body("id", notNullValue())
            .extract().path("id");

        // Second upsert for the same subject must update the same row, not create a new one.
        given()
            .contentType("application/json")
            .body("{\"subject_type\":\"STATUS\",\"subject_id\":\"" + subjectId + "\",\"retention_days\":60}")
        .when()
            .post("/api/v1/retention-policies")
        .then()
            .statusCode(200)
            .body("id", equalTo(id))
            .body("retention_days", equalTo(60));

        given()
        .when()
            .get("/api/v1/retention-policies/" + id)
        .then()
            .statusCode(200)
            .body("retention_days", equalTo(60));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void getByIdReturns404WhenMissing() {
        given()
        .when()
            .get("/api/v1/retention-policies/999999")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void deleteReturns404WhenMissing() {
        given()
        .when()
            .delete("/api/v1/retention-policies/999999")
        .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void purgeLocationReturnsDeletedCount() {
        given()
            .contentType("application/json")
            .queryParam("subject_type", "LOCATION")
        .when()
            .post("/api/v1/retention-policies/purge")
        .then()
            .statusCode(200)
            .body("deleted", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin-user", roles = "admins")
    void purgeStatusReturnsDeletedCount() {
        given()
            .contentType("application/json")
            .queryParam("subject_type", "STATUS")
        .when()
            .post("/api/v1/retention-policies/purge")
        .then()
            .statusCode(200)
            .body("deleted", notNullValue());
    }
}
