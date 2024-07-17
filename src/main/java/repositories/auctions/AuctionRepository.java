package repositories.auctions;

import java.util.List;
import controllers.auctions.Auction;
import controllers.auctions.CreateAuctionRequest;
import controllers.auctions.DeleteAuctionRequest;

public interface AuctionRepository {

    public List<Auction> findAll();

    public Auction getById(int id);

    public Auction create(CreateAuctionRequest request);

    public Auction delete(DeleteAuctionRequest request);
    
}
