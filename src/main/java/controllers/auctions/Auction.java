package controllers.auctions;

import java.time.LocalDateTime;

public class Auction {

    public Integer id;
    public String item_name;
    public String description;
    public LocalDateTime auction_start;
    public LocalDateTime auction_end;
    public String image_path;

    public Auction() {
    }

    public Auction(
            Integer id,
            String item_name,
            String description,
            LocalDateTime auction_start,
            LocalDateTime auction_end,
            String image_path) {
        this.id = id;
        this.item_name = item_name;
        this.description = description;
        this.auction_start = auction_start;
        this.auction_end = auction_end;
        this.image_path = image_path;
    }
}
