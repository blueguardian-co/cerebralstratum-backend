package controllers.auctions;

import java.time.LocalDateTime;

public class DeleteAuctionRequest {

    public DeleteAuctionRequest(
        int id
    ) {
        this.id = id;
    }

    public int id;
}
