package api.v1.bids;

import java.time.LocalDateTime;

import api.v1.user.User;
import repositories.auctions.AuctionEntity;

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
import jakarta.persistence.CascadeType;

@Entity
@Table(name = "bids")
@NamedQuery(
    name = "Bids.findAll",
    query = "SELECT b FROM Bids b ORDER BY b.auction",
    hints = @QueryHint(name = "org.hibernate.cacheable",
    value = "true")
)
@NamedQuery(
    name = "Bids.byUser",
    query = "SELECT b FROM Bids b WHERE b.user.id = :userId ORDER BY b.auction",
    hints = @QueryHint(name = "org.hibernate.cacheable", value = "true")
)
@NamedQuery(
    name = "HighestBids.findAll",
    query = "SELECT b FROM Bids b ORDER BY b.auction.id ASC, b.bid_amount DESC, b.bid_time ASC",
    hints = @QueryHint(name = "org.hibernate.cacheable",
    value = "true")
    
)

@Cacheable
public class Bids {

    @Id
    @SequenceGenerator(name = "bidsSequence", sequenceName = "bids_id_seq", allocationSize = 1, initialValue = 10)
    @GeneratedValue(generator = "bidsSequence")
    private Integer id;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private AuctionEntity auction;

    @ManyToOne(cascade = CascadeType.ALL)
    @PrimaryKeyJoinColumn
    private User user;

    @Column(columnDefinition="timestamp")
    private LocalDateTime bid_time;

    @Column
    private int bid_amount;

    public Bids() {
    }

    public Bids(
        AuctionEntity auction,
        User user,
        LocalDateTime bid_time,
        int bid_amount
    ) {
        this.auction = auction;
        this.user = user;
        this.bid_time = bid_time;
        this.bid_amount = bid_amount;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public AuctionEntity getAuction() {
        return auction;
    }

    public void setAuction(AuctionEntity auction) {
        this.auction = auction;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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