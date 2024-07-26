package controllers.bids;

import java.time.LocalDateTime;

public class CreateBidRequest {

    public int user_id;
    public LocalDateTime bid_time;
    public int bid_amount;

    public CreateBidRequest() {
    }

    public CreateBidRequest(
            int user_id,
            LocalDateTime bid_time,
            int bid_amount) {
        this.user_id = user_id;
        this.bid_time = bid_time;
        this.bid_amount = bid_amount;
    }

}
