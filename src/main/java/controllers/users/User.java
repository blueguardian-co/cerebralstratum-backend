package controllers.users;

public class User {

    public int id;
    public String username;
    public String first_name;
    public String last_name;
    public int table_number;

    public User (
        int id,
        String username,
        String first_name,
        String last_name,
        int table_number
    ) {
        this.id = id;
        this.username = username;
        this.first_name = first_name;
        this.last_name = last_name;
        this.table_number = table_number;
    }
}
