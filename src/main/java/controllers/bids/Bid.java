package controllers.bids;

import java.time.LocalDateTime;

public class Bid {

    public int id;
    public int auction_id;
    public int user_id;
    public LocalDateTime bid_time;
    public int bid_amount;

    public Bid(
        int id,
        int auction_id,
        int user_id,
        LocalDateTime bid_time,
        int bid_amount
    ) {
        this.id = id;
        this.auction_id = auction_id;
        this.user_id = user_id;
        this.bid_time = bid_time;
        this.bid_amount = bid_amount;
    }
}
