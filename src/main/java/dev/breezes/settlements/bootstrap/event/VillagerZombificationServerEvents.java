package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerGeneticsAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerWasCuredAttachment;
import dev.breezes.settlements.infrastructure.minecraft.attachments.ZombieSettlementsOriginAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion.VillagerConversionUtil;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.genetics.VillagerGeneticAttributes;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

import javax.inject.Inject;

/**
 * Handles the two-phase villager ↔ ZombieVillager conversion loop so that a cured zombie that
 * originally was a Settlements villager comes back as a Settlements villager rather than a vanilla one.
 * <p>
 * Zombification pass: Settlements villager → ZombieVillager. We stamp a serialized origin marker so the
 * cure phase can recognize the zombie after an arbitrary server restarts and carry genetics across
 * the zombie phase (genetics is DNA, not memory — the brain was eaten, but the genes survived).
 * <p>
 * Curing pass: ZombieVillager → vanilla Villager. Vanilla always converts to vanilla villager,
 * we intercept post-conversion, swap the vanilla villager for a new Settlements villager built from the
 * same NBT (which includes cure-discount gossip, profession, trades, custom name, XP, and baby flag),
 * then overlay the preserved genetics, and record the 'was-cured' fact.
 * <p>
 * Villagers that were not Settlements villager (natural or igloo zombie villagers) are left as vanilla.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
@CustomLog
public final class VillagerZombificationServerEvents {

    @SubscribeEvent
    public void onLivingConversionPost(LivingConversionEvent.Post event) {
        LivingEntity original = event.getEntity();
        LivingEntity outcome = event.getOutcome();

        // Only meaningful on the server; entities exist on both sides after NeoForge fires the event.
        if (!(original.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (original instanceof BaseVillager baseVillager && outcome instanceof ZombieVillager zombie) {
            handleZombificationConversion(baseVillager, zombie);
        } else if (original instanceof ZombieVillager zombie && outcome instanceof Villager vanillaVillager) {
            handleCuringConversion(serverLevel, zombie, vanillaVillager);
        }
    }

    /**
     * Stamp the origin marker and carry genetics across the zombie phase so the cure handler can
     * restore the individual's DNA even after save/unload cycles.
     */
    private void handleZombificationConversion(BaseVillager baseVillager, ZombieVillager zombie) {
        ZombieSettlementsOriginAttachment.markAsSettlementsOrigin(zombie);
        VillagerGeneticsAttachment.saveFrom(zombie, baseVillager.getGenetics());
        log.debug("Settlements villager zombified — origin marker and genetics carried to zombie entity {}", zombie.getUUID());
    }

    /**
     * If the zombie did not originate from a Settlements villager, we leave the vanilla villager in place.
     * Otherwise, we swap the vanilla villager for a new Settlements villager (using the same NBT round-trip
     * that VillagerConversionUtil uses elsewhere), overlay the preserved genetics, and mark the
     * was-cured core memory.
     */
    private void handleCuringConversion(ServerLevel serverLevel, ZombieVillager zombie, Villager vanillaVillager) {
        if (!ZombieSettlementsOriginAttachment.isSettlementsOrigin(zombie)) {
            return;
        }

        // Read the genetics the forward handler deposited on the zombie.
        GeneticsProfile preservedGenetics = new GeneticsProfile();
        boolean hadGenetics = VillagerGeneticsAttachment.loadInto(zombie, preservedGenetics);

        // VillagerConversionUtil.convertToSettlements performs the NBT round-trip:
        //   saveWithoutId → create BaseVillager → load → addFreshEntity → discard vanillaVillager
        // The round-trip carries cure-discount gossip, profession, trades, custom name, XP, and baby age.
        BaseVillager newVillager = VillagerConversionUtil.convertToSettlements(serverLevel, vanillaVillager);
        if (newVillager == null) {
            log.warn("convertToSettlements returned null for cured zombie {} — leaving as vanilla villager", zombie.getUUID());
            return;
        }

        if (hadGenetics) {
            // Preserve genetics
            newVillager.getGenetics().replaceWith(preservedGenetics);
            VillagerGeneticAttributes.apply(newVillager);
            VillagerGeneticsAttachment.saveFrom(newVillager, newVillager.getGenetics());
        }

        // Durable core-memory fact: this villager survived zombification and was cured.
        VillagerWasCuredAttachment.markAsCured(newVillager);

        log.debug("Cured zombie {} restored as BaseVillager {} (genetics restored={})",
                zombie.getUUID(), newVillager.getUUID(), hadGenetics);
    }

}
