package dev.breezes.settlements.application.ai.trading;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.item.Item;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Mutable trade session shared by the two trade behaviors.
 * <p>
 * The initiator is the sole writer for all state transitions and field mutations.
 * This class is intentionally mutable, so the active session can evolve without creating a new object
 * every tick, but that also means references are not safe to cache, share across threads, or expose
 * to client-side code.
 */
@Getter
public final class TradeSession {

    private final UUID sessionId;
    private final UUID initiatorId;
    private final UUID responderId;
    private final UUID buyerId;
    private final UUID sellerId;
    private final Item matchedItem;
    private final int bundleSize;
    private final int priceJitter;

    private int buyerOffer;
    private int sellerAsk;
    private int negotiationRoundsRemaining;
    private TradeSessionPhase phase;
    private long phaseEnteredGameTime;

    @Builder
    public TradeSession(@Nonnull UUID sessionId,
                        @Nonnull UUID initiatorId,
                        @Nonnull UUID responderId,
                        @Nonnull UUID buyerId,
                        @Nonnull UUID sellerId,
                        @Nonnull Item matchedItem,
                        int bundleSize,
                        int priceJitter,
                        int buyerOffer,
                        int sellerAsk,
                        int negotiationRoundsRemaining,
                        @Nonnull TradeSessionPhase phase,
                        long phaseEnteredGameTime) {
        if (bundleSize <= 0) {
            throw new IllegalArgumentException("Trade session bundleSize must be positive");
        }
        if (priceJitter < 0) {
            throw new IllegalArgumentException("Trade session priceJitter must be non-negative");
        }
        if (buyerOffer <= 0) {
            throw new IllegalArgumentException("Trade session buyerOffer must be positive");
        }
        if (sellerAsk <= 0) {
            throw new IllegalArgumentException("Trade session sellerAsk must be positive");
        }
        if (negotiationRoundsRemaining < 0) {
            throw new IllegalArgumentException("Trade session negotiationRoundsRemaining must be non-negative");
        }

        this.sessionId = sessionId;
        this.initiatorId = initiatorId;
        this.responderId = responderId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.matchedItem = matchedItem;
        this.bundleSize = bundleSize;
        this.priceJitter = priceJitter;
        this.buyerOffer = buyerOffer;
        this.sellerAsk = sellerAsk;
        this.negotiationRoundsRemaining = negotiationRoundsRemaining;
        this.phase = phase;
        this.phaseEnteredGameTime = phaseEnteredGameTime;
    }

    public void setBuyerOffer(int buyerOffer) {
        if (buyerOffer <= 0) {
            throw new IllegalArgumentException("Trade session buyerOffer must be positive");
        }
        this.buyerOffer = buyerOffer;
    }

    public void setSellerAsk(int sellerAsk) {
        if (sellerAsk <= 0) {
            throw new IllegalArgumentException("Trade session sellerAsk must be positive");
        }
        this.sellerAsk = sellerAsk;
    }

    public void setNegotiationRoundsRemaining(int negotiationRoundsRemaining) {
        if (negotiationRoundsRemaining < 0) {
            throw new IllegalArgumentException("Trade session negotiationRoundsRemaining must be non-negative");
        }
        this.negotiationRoundsRemaining = negotiationRoundsRemaining;
    }

    public void transitionTo(@Nonnull TradeSessionPhase nextPhase, long currentGameTime) {
        this.phase = nextPhase;
        this.phaseEnteredGameTime = currentGameTime;
    }

}
