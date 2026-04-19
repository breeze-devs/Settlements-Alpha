package dev.breezes.settlements.application.ai.trading;

import dev.breezes.settlements.shared.util.RandomUtil;
import lombok.CustomLog;

@CustomLog
public final class NegotiationEngine {

    public enum ConcedingSide {
        BUYER,
        SELLER,
    }

    public record NegotiationRoundResult(
            int buyerOffer,
            int sellerAsk
    ) {
    }

    public NegotiationRoundResult advanceRound(int buyerOffer, int sellerAsk, int priceJitter) {
        if (buyerOffer <= 0 || sellerAsk <= 0) {
            throw new IllegalArgumentException("Negotiation prices must be positive");
        }
        if (priceJitter < 0) {
            throw new IllegalArgumentException("Negotiation priceJitter must be non-negative");
        }
        if (buyerOffer >= sellerAsk) {
            throw new IllegalArgumentException("Negotiation advance requires a positive spread");
        }

        log.info("Negotiation round start: buyerOffer={}, sellerAsk={}, priceJitter={}", buyerOffer, sellerAsk, priceJitter);

        int spread = sellerAsk - buyerOffer;
        int buyerConcession = this.randomConcessionAmount(spread);
        int sellerConcession = this.randomConcessionAmount(spread);

        // Mutual concessions keep both villagers visibly participating in every round. We still
        // clamp against the opposite side so the shared price state can converge without crossing
        // into invalid ranges before the deal guard handles the terminal condition.
        int nextBuyerOffer = Math.min(buyerOffer + buyerConcession, sellerAsk);
        int nextSellerAsk = Math.max(nextBuyerOffer, sellerAsk - sellerConcession);

        log.info("Negotiation round result: nextBuyerOffer={}, nextSellerAsk={}, buyerConcession={}, sellerConcession={}",
                nextBuyerOffer, nextSellerAsk, buyerConcession, sellerConcession);
        return new NegotiationRoundResult(nextBuyerOffer, nextSellerAsk);
    }

    private int randomConcessionAmount(int spread) {
        int minimumConcession;
        int maximumConcession;

        // Big spreads should move proportionally so negotiations do not drag forever, but once the
        // villagers are close to agreement they should stop nickel-and-diming each other over a
        // single emerald. The tiered curve keeps late-stage haggling feeling decisive.
        if (spread >= 20) {
            minimumConcession = Math.max(1, (int) Math.ceil(spread * 0.05D));
            maximumConcession = Math.max(minimumConcession, (int) Math.floor(spread * 0.20D));
        } else if (spread >= 10) {
            minimumConcession = Math.max(2, (int) Math.ceil(spread * 0.10D));
            maximumConcession = Math.max(minimumConcession, (int) Math.floor(spread * 0.25D));
        } else {
            minimumConcession = Math.clamp((int) Math.ceil(spread * 0.20D), 2, spread);
            maximumConcession = Math.max(minimumConcession, (int) Math.floor(spread * 0.40D));
        }

        maximumConcession = Math.min(spread, maximumConcession);
        return RandomUtil.randomInt(minimumConcession, maximumConcession, true);
    }

}
