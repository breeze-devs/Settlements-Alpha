package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.application.settlement.membership.SettlementMembershipService;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.personality.OriginType;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerWasCuredAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.ZombieSettlementsOriginAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion.VillagerConversionUtil;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

/**
 * Carries Settlements villagers through vanilla's villager conversions, and records each conversion of a settlement
 * member in its settlement.
 * <p>
 * A Settlements villager that zombifies leaves a zombie villager marked as one, so that curing it, after any number of
 * reloads, brings back a Settlements villager rather than a vanilla one. The returning villager keeps what the cured
 * villager saved, such as its cure discounts and trades, and rejoins the settlement the zombie belonged to. A cure of
 * any other zombie villager stays vanilla.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class VillagerConversionServerEvents {

    private final SettlementMembershipService membershipService;

    // Below the default priority, so the attachments copied on conversion already sit on the vanilla villager the
    // cure's NBT round trip reads
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onLivingConversionPost(LivingConversionEvent.Post event) {
        LivingEntity original = event.getEntity();
        LivingEntity outcome = event.getOutcome();

        // Only meaningful on the server; entities exist on both sides after NeoForge fires the event.
        if (!(original.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        switch (original) {
            case BaseVillager villager when outcome instanceof ZombieVillager zombie ->
                    this.handleZombification(villager, zombie);
            case BaseVillager villager when outcome instanceof Witch ->
                    this.membershipService.recordConversion(villager, SettlementLogType.BECAME_WITCH);
            case ZombieVillager zombie when outcome instanceof Villager vanillaVillager ->
                    this.handleCure(serverLevel, zombie, vanillaVillager);
            default -> {
            }
        }
    }

    private void handleZombification(BaseVillager villager, ZombieVillager zombie) {
        // The marker is what lets the cure recognize the zombie after reloads
        ZombieSettlementsOriginAttachment.markAsSettlementsOrigin(zombie);
        this.membershipService.recordConversion(villager, SettlementLogType.ZOMBIFIED);
        log.debug("Settlements villager {} zombified as {}", villager.getUUID(), zombie.getUUID());
    }

    private void handleCure(ServerLevel serverLevel, ZombieVillager zombie, Villager vanillaVillager) {
        if (!ZombieSettlementsOriginAttachment.isSettlementsOrigin(zombie)) {
            return;
        }

        BaseVillager newVillager = VillagerConversionUtil.convertToSettlements(serverLevel, vanillaVillager,
                OriginType.ZOMBIE_CONVERTED);
        if (newVillager == null) {
            log.warn("convertToSettlements returned null for cured zombie {} — leaving as vanilla villager", zombie.getUUID());
            return;
        }

        // Durable core-memory fact: this villager survived zombification and was cured
        VillagerWasCuredAttachment.markAsCured(newVillager);
        this.membershipService.recordCure(newVillager, zombie);

        log.debug("Cured zombie {} restored as BaseVillager {}", zombie.getUUID(), newVillager.getUUID());
    }

}
