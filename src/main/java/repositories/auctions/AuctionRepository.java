package repositories.auctions;

import java.util.List;
import controllers.auctions.Auction;
import controllers.auctions.CreateAuctionRequest;

public interface AuctionRepository {

    public List<Auction> findAll();

    public Auction getById(int id);

    public Auction create(CreateAuctionRequest request);
    
}
