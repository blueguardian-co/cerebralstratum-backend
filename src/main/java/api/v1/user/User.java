package api.v1.user;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_info")
@NamedQuery(name = "User.findAll", query = "SELECT a FROM User a ORDER BY a.username", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Cacheable
public class User {

    @Id
    @SequenceGenerator(name = "userSequence", sequenceName = "user_info_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "userSequence")
    private Integer id;

    @Column(length = 255, unique = true)
    private String username;

    @Column(length = 3)
    private int table_number;

    public User() {
    }

    public User(
        String username,
        int table_number
    ) {
        this.username = username;
        this.table_number = table_number;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getTable_number() {
        return table_number;
    }

    public void setTable_number(int table_number) {
        this.table_number = table_number;
    }
}