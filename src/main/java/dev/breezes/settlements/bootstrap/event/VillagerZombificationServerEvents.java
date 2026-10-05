package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.personality.OriginType;
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
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

/**
 * Handles the two-phase villager ↔ ZombieVillager conversion loop so that a cured zombie that
 * originally was a Settlements villager comes back as a Settlements villager rather than a vanilla one.
 * <p>
 * Zombification pass: Settlements villager → ZombieVillager. We stamp a serialized origin marker so the
 * cure phase can recognize the zombie after any number of server restarts. The genes ride across the zombie
 * phase as an attachment copied on conversion (genetics is DNA, not memory — the brain was eaten, but the
 * genes survived).
 * <p>
 * Curing pass: ZombieVillager → vanilla Villager. Vanilla always converts to vanilla villager,
 * we intercept post-conversion, swap the vanilla villager for a new Settlements villager built from the
 * same NBT (which includes cure-discount gossip, profession, trades, custom name, XP, baby flag and the
 * genes), and record the 'was-cured' fact.
 * <p>
 * Villagers that were not Settlements villager (natural or igloo zombie villagers) are left as vanilla.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class VillagerZombificationServerEvents {

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

        if (original instanceof BaseVillager && outcome instanceof ZombieVillager zombie) {
            handleZombificationConversion(zombie);
        } else if (original instanceof ZombieVillager zombie && outcome instanceof Villager vanillaVillager) {
            handleCuringConversion(serverLevel, zombie, vanillaVillager);
        }
    }

    /**
     * Stamp the origin marker so the cure handler can recognize the zombie even after save/unload cycles.
     */
    private void handleZombificationConversion(ZombieVillager zombie) {
        ZombieSettlementsOriginAttachment.markAsSettlementsOrigin(zombie);
        log.debug("Settlements villager zombified — origin marker carried to zombie entity {}", zombie.getUUID());
    }

    /**
     * If the zombie did not originate from a Settlements villager, we leave the vanilla villager in place.
     * Otherwise, we swap the vanilla villager for a new Settlements villager (using the same NBT round-trip
     * that VillagerConversionUtil uses elsewhere) and mark the was-cured core memory.
     */
    private void handleCuringConversion(ServerLevel serverLevel, ZombieVillager zombie, Villager vanillaVillager) {
        if (!ZombieSettlementsOriginAttachment.isSettlementsOrigin(zombie)) {
            return;
        }

        // The NBT round-trip carries cure-discount gossip, profession, trades, custom name, XP, baby age and genes
        BaseVillager newVillager = VillagerConversionUtil.convertToSettlements(serverLevel, vanillaVillager, OriginType.ZOMBIE_CONVERTED);
        if (newVillager == null) {
            log.warn("convertToSettlements returned null for cured zombie {} — leaving as vanilla villager", zombie.getUUID());
            return;
        }

        // Durable core-memory fact: this villager survived zombification and was cured
        VillagerWasCuredAttachment.markAsCured(newVillager);

        log.debug("Cured zombie {} restored as BaseVillager {}", zombie.getUUID(), newVillager.getUUID());
    }

}
