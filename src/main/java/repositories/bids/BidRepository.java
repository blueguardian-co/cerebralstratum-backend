package repositories.bids;

import java.util.List;
import controllers.bids.Bid;
import controllers.bids.CreateBidRequest;

public interface BidRepository {

    public List<Bid> findAll(int auction_id);

    public Bid getById(int id);

    public List<Bid> getByUser(int user_id);

    public List<Bid> getByUserAndAuction(int auction_id, int user_id);

    public Bid getHighest(int auction_id);

    public Bid create(int auction_id, CreateBidRequest request);

    public Bid delete(int bid_id);
    
}