package dev.breezes.settlements.application.ai.behavior.usecases.villager.trading;

import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.catalog.TradePriceResolver;
import dev.breezes.settlements.application.economy.demand.ActiveDemand;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.economy.catalog.ItemMatch;
import dev.breezes.settlements.domain.economy.catalog.OfferEntry;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.CustomLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class PartnerScanner {

    private final TradeCatalogRegistry tradeCatalogRegistry;
    private final TradePriceResolver tradePriceResolver;
    private final VillagerWallet villagerWallet;

    public Optional<TradeCandidate> findPartner(@Nonnull BaseVillager buyer,
                                                @Nonnull List<ActiveDemand> activeDemands,
                                                int scanRadiusBlocks) {
        if (activeDemands.isEmpty()) {
            return Optional.empty();
        }

        AABB searchBounds = buyer.getBoundingBox().inflate(scanRadiusBlocks, 6, scanRadiusBlocks);
        Predicate<BaseVillager> predicate = candidate -> candidate != buyer && candidate.isTradeAvailable();
        List<BaseVillager> candidates = buyer.level().getEntitiesOfClass(BaseVillager.class, searchBounds, predicate);

        log.behaviorStatus("Scanning {} trade candidates for buyer {} across {} active demands",
                candidates.size(), buyer.getUUID(), activeDemands.size());

        for (ActiveDemand activeDemand : activeDemands) {
            log.behaviorStatus("Trying to satisfy active demand '{}' for buyer {}", activeDemand.match().asDebugString(), buyer.getUUID());
            for (BaseVillager seller : candidates) {
                Optional<TradeCandidate> candidate = this.matchCandidate(buyer, seller, activeDemand);
                if (candidate.isPresent()) {
                    return candidate;
                }
            }
        }

        return Optional.empty();
    }

    private Optional<TradeCandidate> matchCandidate(@Nonnull BaseVillager buyer,
                                                    @Nonnull BaseVillager seller,
                                                    @Nonnull ActiveDemand activeDemand) {
        // TODO: make this a getter in BaseVillager
        VillagerProfessionKey sellerProfession = VillagerProfessionKey.fromResourceLocation(
                BuiltInRegistries.VILLAGER_PROFESSION.getKey(seller.getVillagerData().getProfession())
        );
        VillagerInventory sellerInventory = seller.getSettlementsInventory();
        int buyerBalance = this.villagerWallet.getBalance(buyer);

        List<OfferEntry> offers = this.tradeCatalogRegistry.findOffers(sellerProfession, activeDemand.match());
        if (offers.isEmpty()) {
            log.behaviorTrace("Rejecting seller {} for active demand '{}': profession {} has no matching offers",
                    seller.getUUID(), activeDemand.match().asDebugString(), sellerProfession.id());
            return Optional.empty();
        }

        for (OfferEntry offerEntry : offers) {
            for (ItemStack stack : sellerInventory.getBackpack().getItems()) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (!matchesItemMatch(stack, activeDemand.match()) || !matchesItemMatch(stack, offerEntry.match())) {
                    continue;
                }

                Item matchedItem = stack.getItem();
                int sellerCount = sellerInventory.countItem(matchedItem);
                int minimumStock = Math.max(offerEntry.bundleSize(), offerEntry.surplusThreshold());
                if (sellerCount < minimumStock) {
                    log.behaviorTrace("Rejecting seller {} for item {}: stock {} is below minimum {} (bundle={}, surplusThreshold={})",
                            seller.getUUID(), BuiltInRegistries.ITEM.getKey(matchedItem), sellerCount,
                            minimumStock, offerEntry.bundleSize(), offerEntry.surplusThreshold());
                    continue;
                }

                ItemMatch priceMatch = new ItemMatch.ItemRef(BuiltInRegistries.ITEM.getKey(matchedItem));

                // Price resolution is anchored to the concrete stack selected from the seller inventory.
                // This keeps cross-kind want matching (#tag vs item) from drifting during pricing.
                int sellerAsk = this.tradePriceResolver.resolveOfferBundlePrice(seller, priceMatch)
                        .orElse(-1);
                int buyerOffer = Math.min(activeDemand.basePricePerUnit() * offerEntry.bundleSize(), buyerBalance);
                if (sellerAsk <= 0) {
                    log.behaviorTrace("Rejecting seller {} for item {}: no offer price resolved from concrete match {}",
                            seller.getUUID(), BuiltInRegistries.ITEM.getKey(matchedItem), priceMatch.asDebugString());
                    continue;
                }
                if (buyerOffer <= 0) {
                    log.behaviorTrace("Rejecting seller {} for item {}: buyer {} has no demand price or affordable offer (balance={})",
                            seller.getUUID(), BuiltInRegistries.ITEM.getKey(matchedItem), buyer.getUUID(), buyerBalance);
                    continue;
                }

                log.behaviorStatus("Matched trade candidate: buyer={}, seller={}, item={}, want={}, sellerAsk={}, buyerOffer={}, bundleSize={}",
                        buyer.getUUID(), seller.getUUID(), BuiltInRegistries.ITEM.getKey(matchedItem), activeDemand.match().asDebugString(),
                        sellerAsk, buyerOffer, offerEntry.bundleSize());
                return Optional.of(TradeCandidate.builder()
                        .partner(seller)
                        .activeDemand(activeDemand)
                        .matchedItem(matchedItem)
                        .offerEntry(offerEntry)
                        .buyerOffer(buyerOffer)
                        .sellerAsk(sellerAsk)
                        .build());
            }
        }

        log.behaviorStatus("Rejecting seller {} for active demand '{}': no inventory stack satisfied both demand and offer rules",
                seller.getUUID(), activeDemand.match().asDebugString());

        return Optional.empty();
    }

    private static boolean matchesItemMatch(@Nonnull ItemStack stack, @Nonnull ItemMatch itemMatch) {
        return switch (itemMatch) {
            case ItemMatch.ItemRef itemRef -> stack.is(BuiltInRegistries.ITEM.get(itemRef.id()));
            case ItemMatch.TagRef tagRef -> stack.is(tagRef.tag());
        };
    }

    @Builder
    public record TradeCandidate(@Nonnull BaseVillager partner,
                                 @Nonnull ActiveDemand activeDemand,
                                 @Nonnull Item matchedItem,
                                 @Nonnull OfferEntry offerEntry,
                                 int buyerOffer,
                                 int sellerAsk) {
    }

}
