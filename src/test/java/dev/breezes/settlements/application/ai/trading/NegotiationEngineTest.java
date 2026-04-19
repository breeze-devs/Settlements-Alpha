package dev.breezes.settlements.application.ai.trading;

import org.junit.jupiter.api.Test;

import dev.breezes.settlements.shared.util.RandomUtil;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NegotiationEngineTest {

    private final NegotiationEngine engine = new NegotiationEngine();

    @Test
    void advanceRound_convergesWhenOverlapExists() {
        RandomUtil.RANDOM.setSeed(1234L);
        int buyerOffer = 8;
        int sellerAsk = 12;

        for (int i = 0; i < 4 && buyerOffer < sellerAsk; i++) {
            NegotiationEngine.NegotiationRoundResult result = this.engine.advanceRound(buyerOffer, sellerAsk, 0);
            buyerOffer = result.buyerOffer();
            sellerAsk = result.sellerAsk();
        }

        assertTrue((sellerAsk - buyerOffer) <= 1);
    }

    @Test
    void advanceRound_reportsConcedingSide() {
        RandomUtil.RANDOM.setSeed(1234L);
        NegotiationEngine.NegotiationRoundResult result = this.engine.advanceRound(10, 15, 0);

        assertTrue(result.buyerOffer() > 10);
        assertTrue(result.sellerAsk() < 15);
    }

    @Test
    void advanceRound_neverPushesPricesAwayFromEachOther() {
        RandomUtil.RANDOM.setSeed(4321L);
        int buyerOffer = 10;
        int sellerAsk = 18;

        for (int i = 0; i < 6 && buyerOffer < sellerAsk; i++) {
            int previousSpread = sellerAsk - buyerOffer;
            NegotiationEngine.NegotiationRoundResult result = this.engine.advanceRound(buyerOffer, sellerAsk, 3);
            buyerOffer = result.buyerOffer();
            sellerAsk = result.sellerAsk();
            assertTrue((sellerAsk - buyerOffer) < previousSpread);
        }
    }

    @Test
    void advanceRound_concessionAmountStaysWithinConfiguredSpreadFraction() {
        RandomUtil.RANDOM.setSeed(99L);

        int buyerOffer = 10;
        int sellerAsk = 30;
        int spread = sellerAsk - buyerOffer;
        int minimumConcession = (int) Math.ceil(spread * 0.05D);
        int maximumConcession = (int) Math.floor(spread * 0.20D);

        NegotiationEngine.NegotiationRoundResult result = this.engine.advanceRound(buyerOffer, sellerAsk, 0);
        int buyerConcessionAmount = result.buyerOffer() - buyerOffer;
        int sellerConcessionAmount = sellerAsk - result.sellerAsk();

        assertTrue(buyerConcessionAmount >= minimumConcession);
        assertTrue(buyerConcessionAmount <= maximumConcession);
        assertTrue(sellerConcessionAmount >= minimumConcession);
        assertTrue(sellerConcessionAmount <= maximumConcession);
    }

    @Test
    void advanceRound_smallSpreadUsesMoreDecisiveConcessions() {
        RandomUtil.RANDOM.setSeed(7L);

        NegotiationEngine.NegotiationRoundResult result = this.engine.advanceRound(20, 28, 0);

        assertTrue((result.buyerOffer() - 20) >= 2);
        assertTrue((28 - result.sellerAsk()) >= 2);
    }

}
