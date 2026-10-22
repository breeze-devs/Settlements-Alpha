package dev.breezes.settlements.application.ai.sensors;

import dev.breezes.settlements.application.ai.behavior.usecases.villager.trading.PartnerScanner;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.domain.ai.memory.IMemoryWrite;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.memory.MemoryWrite;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.Tickable;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Scans for any free villagers that have this villager's desired items and are free to trade.
 */
public final class PurchasableDemandedItemSensor extends AbstractSensor<BaseVillager> {

    private final PartnerScanner partnerScanner;

    public PurchasableDemandedItemSensor(@Nonnull TradingConfig config, @Nonnull PartnerScanner partnerScanner) {
        super(List.of(), Tickable.staggered(ClockTicks.seconds(config.partnerScanIntervalSeconds())));
        this.partnerScanner = partnerScanner;
    }

    @Override
    public List<IMemoryWrite> doSense(@Nonnull Level world, @Nonnull BaseVillager entity) {
        return List.of(this.partnerScanner.findPartner(entity)
                .<IMemoryWrite>map(candidate -> MemoryWrite.of(MemoryTypeRegistry.DEMANDED_ITEM_SELLER, candidate.partner()))
                .orElseGet(() -> MemoryWrite.clear(MemoryTypeRegistry.DEMANDED_ITEM_SELLER)));
    }

}
