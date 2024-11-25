package controllers.users;

public class UpdateUserRequest {
    public Integer user_id;
    public Integer organisation_id;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(
            Integer user_id,
            Integer organisation_id
    ) {
        this.user_id = user_id;
        this.organisation_id = organisation_id;
    }
}