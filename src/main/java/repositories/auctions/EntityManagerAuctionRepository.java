package repositories.auctions;

import controllers.auctions.Auction;
import controllers.auctions.CreateAuctionRequest;
import controllers.auctions.DeleteAuctionRequest;
import controllers.auctions.UpdateAuctionRequest;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Default
    @ApplicationScoped
public class EntityManagerAuctionRepository implements AuctionRepository {

    @Inject
    EntityManager entityManager;

    private static Auction mapEntityToAuction (AuctionEntity auction) {
        return new Auction(
            auction.getId(),
            auction.getItem_name(),
            auction.getDescription(),
            auction.getAuction_start(),
            auction.getAuction_end(),
            auction.getImage_path()
        );
    }

    private static AuctionEntity mapCreateRequestToEntity (CreateAuctionRequest request) {
        return new AuctionEntity(
            request.item_name,
            request.description,
            request.auction_start,
            request.auction_end,
            request.image_path
        );
    }

    private static AuctionEntity mapUpdateRequestToEntity (UpdateAuctionRequest request) {
        return new AuctionEntity(
            request.item_name,
            request.description,
            request.auction_start,
            request.auction_end,
            request.image_path
        );
    }

    public List<Auction> findAll() {
        return entityManager.createNamedQuery("AuctionEntity.findAll", AuctionEntity.class)
            .getResultList().stream().map(EntityManagerAuctionRepository::mapEntityToAuction).collect(Collectors.toList());          
    }

    public Auction getById(int id) {
        AuctionEntity auction = entityManager.find(AuctionEntity.class, id);
        return mapEntityToAuction(auction);
    }

    @Transactional
    public Auction create(CreateAuctionRequest request) {
        AuctionEntity newAuction = mapCreateRequestToEntity(request);
        entityManager.persist(newAuction);
        return mapEntityToAuction(newAuction);
    }

    @Transactional
    public Auction delete(DeleteAuctionRequest request) {
        AuctionEntity auction = entityManager.find(AuctionEntity.class, request.id);
        entityManager.remove(auction);
        return mapEntityToAuction(auction);
    }

    @Transactional
    public Auction update(UpdateAuctionRequest request) {
        AuctionEntity updateAuction = mapUpdateRequestToEntity(request);

        // List<User> oidcUserIdList = entityManager.createNamedQuery("User.getUser", User.class)
        //     .setParameter("username", securityIdentity.getPrincipal().getName())
        //     .getResultList();

        // // Get requesting user's ID from the user_info table
        // int oidcUserId = oidcUserIdList.get(0).getId();

        // // Get the requested bid user's ID
        // int bidUserId = bid.getUser().getId();

        // if (oidcUserId != bidUserId) {
        //     throw new WebApplicationException("You can only update bids on your own behalf.", 403);
        // }
        entityManager.merge(updateAuction);
        return mapEntityToAuction(updateAuction);
    }
    
}
