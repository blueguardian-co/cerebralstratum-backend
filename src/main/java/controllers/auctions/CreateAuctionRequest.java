package controllers.auctions;

import java.time.LocalDateTime;

public class CreateAuctionRequest {

    public String item_name;
    public String description;
    public LocalDateTime auction_start;
    public LocalDateTime auction_end;
    public Integer starting_bid;
    public String image_path;

    public CreateAuctionRequest() {
    }

    public CreateAuctionRequest(
            String item_name,
            String description,
            LocalDateTime auction_start,
            LocalDateTime auction_end,
            Integer starting_bid,
            String image_path) {
        this.item_name = item_name;
        this.description = description;
        this.auction_start = auction_start;
        this.auction_end = auction_end;
        this.starting_bid = starting_bid;
        this.image_path = image_path;
    }
}
