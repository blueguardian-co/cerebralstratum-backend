package controllers.users;

public class CreateUserRequest {
    public String username;
    public Integer organisation_id;

    public CreateUserRequest() {
    }

    public CreateUserRequest(
            String username,
            Integer organisation_id
    ) {
        this.username = username;
        this.organisation_id = organisation_id;
    }
}