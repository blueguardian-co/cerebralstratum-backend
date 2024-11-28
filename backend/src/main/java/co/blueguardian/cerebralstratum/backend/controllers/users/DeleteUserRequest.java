package co.blueguardian.cerebralstratum.backend.controllers.users;

public class DeleteUserRequest {
    public int user_id;

    public DeleteUserRequest() {
    }

    public DeleteUserRequest(
            int user_id
    ) {
        this.user_id = user_id;
    }
}