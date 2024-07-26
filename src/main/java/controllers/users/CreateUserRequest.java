package controllers.users;

public class CreateUserRequest {
    public String username;
    public int table_number;

    public CreateUserRequest() {
    }

    public CreateUserRequest(
            String username,
            int table_number) {
        this.username = username;
        this.table_number = table_number;
    }
}