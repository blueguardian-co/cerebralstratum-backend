package controllers.auctions;

import jakarta.enterprise.context.ApplicationScoped;

import repositories.auctions.AuctionRepository;
import controllers.bids.Bid;

import io.quarkus.websockets.next.*;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import repositories.bids.BidRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@ApplicationScoped
@WebSocket(path = "/api/v1/auctions/ws")
public class AuctionWebSocket {
    public enum MessageType {ALL_AUCTIONS, ALL_AUCTIONS_HIGHEST_BIDS, AUCTION_NOTIFICATION}

    public record AllAuctions(MessageType type, List<Auction> auctions) {
    }
    public record AllAuctionsHighestBids(MessageType type, List<Bid> bids) {
    }
    public record AuctionNotification(MessageType type, Integer auction_id, String message) {
    }

    @Inject
    OpenConnections openConnections;

    @Inject
    AuctionRepository auctionRepository;

    @Inject
    BidRepository bidRepository;

    @OnOpen(broadcast = true)
    public AllAuctions onOpen() {
        return new AllAuctions(MessageType.ALL_AUCTIONS, auctionRepository.findAll());
    }

    @OnClose
    public void onClose() {
        // Place a message on the bus?
    }

    @Scheduled(every = "1m")
    public void processAuctionStatus() {
        List<Auction> allAuctions = auctionRepository.findAll();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (Auction auction : allAuctions) {
            if (auction.auction_start.isEqual(now)) {
                auctionNotificationBroadcast(auction, now + ": Auction " + auction.item_name + " is now open for bidding. (" + auction.auction_start + ")");
            } else if (auction.auction_start.isBefore(now.plusMinutes(5)) && auction.auction_start.isAfter(now)) {
                auctionNotificationBroadcast(auction, "Auction " + auction.item_name + " is open for bidding within the next five minutes.");
            }
            if (auction.auction_end.isEqual(now)) {
                auctionNotificationBroadcast(auction, now + ": Auction " + auction.item_name + " is now closed. (" + auction.auction_end + ")");
            } else if (auction.auction_end.isAfter(now.plusMinutes(5)) && auction.auction_end.isAfter(now)) {
                auctionNotificationBroadcast(auction, "Auction " + auction.item_name + " closes within the next five minutes.");
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
        for (WebSocketConnection c : openConnections) {
            c.sendTextAndAwait(
                    new AllAuctionsHighestBids(MessageType.ALL_AUCTIONS_HIGHEST_BIDS, bidRepository.getHighestAllAuctions())
            );
        }
    }
}