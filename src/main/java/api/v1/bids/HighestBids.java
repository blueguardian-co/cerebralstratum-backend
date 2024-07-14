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
@NamedQuery(name = "HighestBids.findAll", query = "SELECT a FROM HighestBids GROUP BY auction_id", hints = @QueryHint(name = "org.hibernate.cacheable", value = "true"))
@Cacheable
public class HighestBids {

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

    public HighestBids() {
    }

    public HighestBids(
        Auctions auction
    ) {
        this.auction = auction;
    }

    public Integer getId() {
        return id;
    }

    public Auctions getAuction() {
        return auction;
    }

}