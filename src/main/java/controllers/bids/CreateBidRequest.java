package controllers.bids;

public class CreateBidRequest {

    public int bid_amount;

    public CreateBidRequest() {
    }

    public CreateBidRequest(
            int bid_amount
    ) {
        this.bid_amount = bid_amount;
    }

}
