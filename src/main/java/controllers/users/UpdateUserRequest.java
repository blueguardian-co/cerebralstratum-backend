package controllers.users;

public class UpdateUserRequest {
    public int user_id;
    public String username;
    public String first_name;
    public String last_name;
    public int table_number;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(
            int user_id,
            String username,
            String first_name,
            String last_name,
            int table_number) {
        this.user_id = user_id;
        this.username = username;
        this.first_name = first_name;
        this.last_name = last_name;
        this.table_number = table_number;
    }
}