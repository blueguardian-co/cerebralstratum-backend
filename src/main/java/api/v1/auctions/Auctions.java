package api.v1.auctions;

import java.time.LocalDateTime;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Lob;

@Entity
@Table(name = "auctions")
@NamedQuery(name = "Auctions.findAll", query = "SELECT a FROM Auctions a ORDER BY a.item_name", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Cacheable
public class Auctions {

    @Id
    @SequenceGenerator(name = "auctionsSequence", sequenceName = "auctions_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "auctionsSequence")
    private Integer id;

    @Column(length = 50, unique = true)
    private String item_name;

    @Column(length = 240, unique = true)
    private String description;

    @Column(columnDefinition="timestamp")
    private LocalDateTime auction_start;

    @Column(columnDefinition="timestamp")
    private LocalDateTime auction_end;

    @Column
    @Lob
    private byte[] image;

    public Auctions() {
    }

    public Auctions(
        String item_name,
        String description,
        LocalDateTime auction_start,
        LocalDateTime auction_end,
        byte[] image
    ) {
        this.item_name = item_name;
        this.description = description;
        this.auction_start = auction_start;
        this.auction_end = auction_end;
        this.image = image;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return item_name;
    }

    public void setName(String item_name) {
        this.item_name = item_name;
    }

}