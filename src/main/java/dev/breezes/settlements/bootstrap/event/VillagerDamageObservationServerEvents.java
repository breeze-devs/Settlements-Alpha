package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.domain.ai.threat.QualifyingHit;
import dev.breezes.settlements.domain.tags.SettlementsDamageTypeTags;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

/**
 * Reports a qualifying hit to a villager's threat response and remembers its attacker as the entity that hurt it.
 * <p>
 * No behavior starts and nothing is scanned inside the event.
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@CustomLog
public final class VillagerDamageObservationServerEvents {

    @SubscribeEvent
    public static void onLivingDamagePost(@Nonnull LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof BaseVillager villager)) {
            return;
        }
        // Damage application is authoritative on the server; the client never owns this state.
        if (!(villager.level() instanceof ServerLevel)) {
            return;
        }
        if (event.getSource().is(SettlementsDamageTypeTags.DOES_NOT_ALARM_VILLAGERS)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (attacker == villager) {
            return;
        }

        LivingEntity livingAttacker = attacker instanceof LivingEntity living ? living : null;
        UUID attackerId = livingAttacker == null ? null : livingAttacker.getUUID();
        Optional<QualifyingHit> hit = villager.getSettlementsBrain().threats()
                .recordHit(attackerId, event.getNewDamage(), villager.level().getGameTime());
        if (hit.isEmpty() || livingAttacker == null) {
            return;
        }

        // Vanilla's flee reads this memory, so it names only an attacker whose hit qualified and lapses with that hit
        villager.getBrain().setMemoryWithExpiry(MemoryModuleType.HURT_BY_ENTITY, livingAttacker,
                QualifyingHit.EXPIRY.getTicks());
        log.debug("Recorded qualifying hit on villager {}: {} health from {}",
                villager.getUUID(), hit.get().healthLost(), hit.get().attackerId());
    }

}
