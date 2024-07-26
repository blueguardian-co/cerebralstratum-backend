package controllers.users;

public class CreateMeRequest {
    public int table_number;

    public CreateMeRequest() {
    }

    public CreateMeRequest(
            int table_number) {
        this.table_number = table_number;
    }
}