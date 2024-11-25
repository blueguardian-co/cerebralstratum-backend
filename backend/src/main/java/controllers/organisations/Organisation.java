package controllers.organisations;

import java.time.LocalDateTime;

public class Organisation {

    public int id;
    public String name;
    public int owner;
    public LocalDateTime created;

    public Organisation (
            int id,
            String name,
            int owner,
            LocalDateTime created
    ) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.created = created;
    }
}
