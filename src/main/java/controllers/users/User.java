package controllers.users;

public class User {

    public int id;
    public String username;
    public int table_number;

    public User (
        int id,
        String username,
        int table_number
    ) {
        this.id = id;
        this.username = username;
        this.table_number = table_number;
    }
}
