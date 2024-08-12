package controllers.auctions;

import jakarta.enterprise.context.ApplicationScoped;

import repositories.auctions.AuctionRepository;
import repositories.bids.BidRepository;
import repositories.users.UserRepository;
import controllers.bids.Bid;
import controllers.users.User;

import io.quarkus.websockets.next.*;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
@WebSocket(path = "/api/v1/auctions/ws")
public class AuctionWebSocket {
    public enum MessageType {ALL_AUCTIONS, ALL_AUCTIONS_HIGHEST_BIDS, AUCTION_NOTIFICATION, TEXT_MESSAGE}

    public record AllAuctions(MessageType type, List<Auction> auctions) {
    }
    public record AllAuctionsHighestBids(MessageType type, List<Bid> bids) {
    }
    public record AuctionNotification(MessageType type, Integer auction_id, String message) {
    }
    public record TextMessage(MessageType type, String message){
    }

    @Inject
    OpenConnections openConnections;

    @Inject
    AuctionRepository auctionRepository;

    @Inject
    BidRepository bidRepository;

    @Inject
    UserRepository userRepository;

    @OnOpen(broadcast = true)
    public AllAuctions onOpen() {
        return new AllAuctions(MessageType.ALL_AUCTIONS, auctionRepository.findAll());
    }

    @OnTextMessage(broadcast = true)
    public TextMessage onTextMessage(TextMessage message) {
        return message;
    }

    @OnClose
    public void onClose() {
        // Place a message on the bus?
    }

    @Scheduled(every = "1m")
    public void processAuctionStatus() {
        List<Auction> allAuctions = auctionRepository.findAll();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES);

        for (Auction auction : allAuctions) {
            LocalDateTime auction_start = auction.auction_start.truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime auction_end = auction.auction_end.truncatedTo(ChronoUnit.MINUTES);

            if (auction_start.isEqual(now)) {
                auctionNotificationBroadcast(auction, "Auction for " + auction.item_name + " is now open for bidding.");
            } else if (auction_start.isAfter(now) && auction_start.isBefore(now.plusMinutes(5))) {
                auctionNotificationBroadcast(auction, "Auction for " + auction.item_name + " will be open for bidding within the next five minutes.");
            }
            if (auction_end.isEqual(now)) {
                auctionNotificationBroadcast(auction, "Auction for " + auction.item_name + " is now closed.");
                try {
                    Bid winningBid = bidRepository.getHighestByAuction(auction.id);
                    User winningUser = userRepository.getById(winningBid.user_id);
                    auctionNotificationBroadcast(auction, "Congratulations " + winningUser.first_name + " " + winningUser.last_name + " on winning " + auction.item_name + "!!!");
                } catch (Exception e) {
                    auctionNotificationBroadcast(auction, "[" + now + "] No bids placed on auction " + auction.item_name);
                }
            } else if (auction_end.isAfter(now) && auction_end.isBefore(now.plusMinutes(5))) {
                auctionNotificationBroadcast(auction, "Auction for " + auction.item_name + " closes within the next five minutes.");
            }
        }
    }

    public void auctionNotificationBroadcast(Auction auction, String message) {
        for (WebSocketConnection c : openConnections) {
            c.broadcast().sendTextAndAwait(
                    new AuctionNotification(MessageType.AUCTION_NOTIFICATION, auction.id, message)
            );
        }
    }

    @Scheduled(every = "${schedule.all-auctions:60s}")
    public void allAuctionsBroadcast() {
        for (WebSocketConnection c : openConnections) {
            c.sendTextAndAwait(
                    new AllAuctions(MessageType.ALL_AUCTIONS, auctionRepository.findAll())
            );
        }
    }

    // Eventually, rather than scheduled, we trigger on a bid being placed
    @Scheduled(every = "${schedule.all-auctions-highest-bids:5s}")
    public void highestBidsBroadcast() {
        List<Bid> highestBidForEachAuction = bidRepository.getHighestAllAuctions();
        if (!highestBidForEachAuction.isEmpty()) {
            for (WebSocketConnection c : openConnections) {
                c.sendTextAndAwait(
                        new AllAuctionsHighestBids(MessageType.ALL_AUCTIONS_HIGHEST_BIDS, highestBidForEachAuction)
                );
            }
        }
    }
}