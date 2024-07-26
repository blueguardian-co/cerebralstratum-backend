package repositories.bids;

import controllers.bids.Bid;
import controllers.bids.CreateBidRequest;

import repositories.auctions.AuctionEntity;
import repositories.users.UserEntity;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Default
    @ApplicationScoped
public class EntityManagerBidRepository implements BidRepository {

    @Inject
    EntityManager entityManager;

    private static Bid mapEntityToBid (BidEntity bid) {
        int auction_id =  bid.getAuction().getId();
        int user_id = bid.getUser().getId();
        return new Bid(
            bid.getId(),
            auction_id,
            user_id,
            bid.getBid_time(),
            bid.getBid_amount()
        );
    }

    private BidEntity mapCreateRequestToEntity (int auction_id, String username, CreateBidRequest request) {
        AuctionEntity auction =  entityManager.find(AuctionEntity.class, auction_id);
        UserEntity user = entityManager.createNamedQuery("UserEntity.getUser", UserEntity.class)
        .setParameter("username", username)
        .getSingleResult();
        LocalDateTime bid_time = LocalDateTime.now();
        return new BidEntity(
            auction,
            user,
            bid_time,
            request.bid_amount
        );
    }

    @Transactional
    public Bid create(int auction_id, String username, CreateBidRequest request) {
        BidEntity newBid = mapCreateRequestToEntity(auction_id, username, request);
        entityManager.persist(newBid);
        return mapEntityToBid(newBid);
    }

    @Transactional
    public Bid delete(int bid_id) {
        BidEntity bid = entityManager.find(BidEntity.class, bid_id);
        entityManager.remove(bid);
        return mapEntityToBid(bid);
    }

    public List<Bid> findAll(int auction_id) {
        return entityManager.createNamedQuery("Bids.findAll", BidEntity.class)
            .setParameter("auctionId", auction_id)
            .getResultList().stream().map(EntityManagerBidRepository::mapEntityToBid).collect(Collectors.toList());          
    }

    public Bid getById(int id) {
        BidEntity bid = entityManager.find(BidEntity.class, id);
        return mapEntityToBid(bid);
    }

    public Bid getHighest(int auction_id) {
        return mapEntityToBid(entityManager.createNamedQuery("Bids.highest", BidEntity.class)
            .setParameter("auctionId", auction_id)
            .getSingleResult()
        );
    }
    public List<Bid> getByUser(int user_id) {
        return entityManager.createNamedQuery("Bids.byUser", BidEntity.class)
            .setParameter("userId", user_id)
            .getResultList().stream().map(EntityManagerBidRepository::mapEntityToBid).collect(Collectors.toList()); 
    }

    public List<Bid> getByUserAndAuction(int auction_id, int user_id) {
        return entityManager.createNamedQuery("Bids.byUserAndAuction", BidEntity.class)
            .setParameter("userId", user_id)
            .setParameter("auctionId", auction_id)
            .getResultList().stream().map(EntityManagerBidRepository::mapEntityToBid).collect(Collectors.toList()); 
    }
}
