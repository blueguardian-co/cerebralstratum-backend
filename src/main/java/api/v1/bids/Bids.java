package api.v1.bids;

import java.time.LocalDateTime;

import api.v1.auctions.Auctions;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "bids")
@NamedQuery(name = "Bids.findAll", query = "SELECT a FROM Bids a ORDER BY a.auction", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Cacheable
public class Bids {

    @Id
    @SequenceGenerator(name = "bidsSequence", sequenceName = "bids_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "bidsSequence")
    private Integer id;

    @ManyToOne
    @PrimaryKeyJoinColumn
    private Auctions auction;

    @Column(length = 255)
    private String username;

    @Column(columnDefinition="timestamp")
    private LocalDateTime bid_time;

    @Column
    private int bid_amount;

    public Bids() {
    }

    public Bids(
        Auctions auction,
        String username,
        LocalDateTime bid_time,
        int bid_amount
    ) {
        this.auction = auction;
        this.username = username;
        this.bid_time = bid_time;
        this.bid_amount = bid_amount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Auctions getAuction() {
        return auction;
    }

    public void setAuction(Auctions auction) {
        this.auction = auction;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getBid_time() {
        return bid_time;
    }

    public void setBid_time(LocalDateTime bid_time) {
        this.bid_time = bid_time;
    }


    public int getBid_amount() {
        return bid_amount;
    }

    public void setBid_amount(int bid_amount) {
        this.bid_amount = bid_amount;
    }
}