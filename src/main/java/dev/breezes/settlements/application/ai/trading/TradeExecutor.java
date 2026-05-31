package dev.breezes.settlements.application.ai.trading;

import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.inject.Inject;

@ServerScope
@CustomLog
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class TradeExecutor {

    private final VillagerWallet villagerWallet;

    public void execute(@Nonnull TradeSession session,
                        @Nonnull BaseVillager buyer,
                        @Nonnull BaseVillager seller) {
        VillagerInventory buyerInventory = buyer.getSettlementsInventory();
        VillagerInventory sellerInventory = seller.getSettlementsInventory();

        this.validateTrade(session, buyer, seller, sellerInventory);

        log.info("Executing trade session {}: buyer={}, seller={}, item={}, bundleSize={}, price={}",
                session.getSessionId(), buyer.getUUID(), seller.getUUID(), session.getMatchedItem(),
                session.getBundleSize(), session.getBuyerOffer());
        this.villagerWallet.transfer(buyer, seller, session.getBuyerOffer());

        int removed = sellerInventory.consume(session.getMatchedItem(), session.getBundleSize());
        if (removed != session.getBundleSize()) {
            throw new IllegalStateException("Validated seller inventory drifted before trade commit");
        }

        buyerInventory.add(new ItemStack(session.getMatchedItem(), session.getBundleSize()));
    }

    private void validateTrade(@Nonnull TradeSession session,
                               @Nonnull BaseVillager buyer,
                               @Nonnull BaseVillager seller,
                               @Nonnull VillagerInventory sellerInventory) {
        if (!buyer.getUUID().equals(session.getBuyerId())) {
            throw new IllegalArgumentException("Trade buyer does not match session buyerId");
        }
        if (!seller.getUUID().equals(session.getSellerId())) {
            throw new IllegalArgumentException("Trade seller does not match session sellerId");
        }
        if (!this.villagerWallet.canAfford(buyer, session.getBuyerOffer())) {
            throw new IllegalArgumentException("Trade buyer cannot afford the agreed price");
        }
        if (sellerInventory.count(session.getMatchedItem()) < session.getBundleSize()) {
            throw new IllegalArgumentException("Trade seller does not have enough matching items");
        }
    }

}
