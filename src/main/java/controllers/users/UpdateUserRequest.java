package controllers.users;

public class UpdateUserRequest {
    public int user_id;
    public String username;
    public int table_number;

    public UpdateUserRequest (
        int user_id,
        String username,
        int table_number
    ) {
        this.user_id = user_id;
        this.username = username;
        this.table_number = table_number;
    }
}