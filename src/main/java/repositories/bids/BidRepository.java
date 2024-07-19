package repositories.bids;

import java.util.List;
import controllers.bids.Bid;
import controllers.bids.CreateBidRequest;
import controllers.bids.DeleteBidRequest;

public interface BidRepository {

    public List<Bid> findAll(int auction_id);

    public Bid getById(int id);

    public List<Bid> getByUser(int user_id);

    public List<Bid> getByUserAndAuction(int auction_id, int user_id);

    public Bid getHighest(int auction_id);

    public Bid create(CreateBidRequest request);

    public Bid delete(DeleteBidRequest request);
    
}