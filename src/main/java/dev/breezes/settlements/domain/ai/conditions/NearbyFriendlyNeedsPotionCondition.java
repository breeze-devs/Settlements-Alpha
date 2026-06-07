package dev.breezes.settlements.domain.ai.conditions;

import com.google.common.base.Predicates;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.ai.perception.PerceivedEntities;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Condition for clerics to find a friendly entity that's in need of a healing potion.
 * Only triggers on hurt entities (health < maxHealth); potion tier is expertise-scaled.
 */
@CustomLog
public class NearbyFriendlyNeedsPotionCondition<T extends BaseVillager> implements IEntityCondition<T> {

    private static final Set<Class<?>> FRIENDLY_ENTITY_CLASSES = Set.of(Player.class, Villager.class, WanderingTrader.class);

    private final int minimumReputation;

    public NearbyFriendlyNeedsPotionCondition(int minimumReputation) {
        this.minimumReputation = minimumReputation;
    }

    @Override
    public boolean test(@Nullable T villager) {
        if (villager == null) {
            log.sensorWarn("Source entity is null, returning empty targets");
            return false;
        }

        Map<Entity, PotionType> map = this.findFriendlyNeedsPotionMap(villager);
        log.sensorStatus("Found {} friendly entities that need a potion", map.size());
        return !map.isEmpty();
    }

    public Map<Entity, PotionType> findFriendlyNeedsPotionMap(@Nonnull BaseVillager villager) {
        Map<Entity, PotionType> result = new HashMap<>();
        this.getPerceivedEntities(villager)
                .ofType(LivingEntity.class, entity -> this.isEligibleCandidate(villager, entity))
                .forEach(entity -> {
                    Optional<PotionType> potionType = this.needsPotion(villager, entity);
                    potionType.ifPresent(type -> result.put(entity, type));

                    log.sensorTrace("Entity '{}' needs potion type: {}", entity.getName(), potionType.orElse(null));
                });

        return result;
    }

    private boolean isEligibleCandidate(@Nonnull BaseVillager villager, @Nonnull LivingEntity entity) {
        if (!entity.isAlive()
                || FRIENDLY_ENTITY_CLASSES.stream().noneMatch(type -> type.isInstance(entity))) {
            return false;
        }

        if (entity.getType() != EntityType.PLAYER) {
            return true;
        }

        int reputation = this.getReputation(villager, (Player) entity);
        if (reputation >= this.minimumReputation) {
            return true;
        }

        log.sensorTrace("Ignoring player '{}' with reputation '{}' below minimum reputation of {}",
                entity.getName(), reputation, this.minimumReputation);
        return false;
    }

    private PerceivedEntities getPerceivedEntities(@Nonnull BaseVillager villager) {
        return villager.getSettlementsBrain()
                .getMemory(MemoryTypeRegistry.NEARBY_SENSED_ENTITIES)
                .orElse(PerceivedEntities.empty());
    }

    private int getReputation(@Nonnull BaseVillager villager, @Nonnull Player player) {
        return villager.getGossips().getReputation(player.getUUID(), Predicates.alwaysTrue());
    }

    private Optional<PotionType> needsPotion(@Nonnull BaseVillager villager, @Nonnull Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }

        // Only act on hurt entities; pick the best tier the cleric's expertise allows
        if (livingEntity.getHealth() >= livingEntity.getMaxHealth()) {
            return Optional.empty();
        }

        int expertiseLevel = villager.getExpertise().getLevel();
        if (!livingEntity.hasEffect(MobEffects.REGENERATION)
                && expertiseLevel >= PotionType.REGENERATION.getRequiredExpertiseLevel()) {
            return Optional.of(PotionType.REGENERATION);
        } else if (expertiseLevel >= PotionType.STRONG_HEALING.getRequiredExpertiseLevel()) {
            return Optional.of(PotionType.STRONG_HEALING);
        } else {
            return Optional.of(PotionType.HEALING);
        }
    }

    @AllArgsConstructor
    @Getter
    public enum PotionType {

        HEALING(1, Potions.HEALING),
        STRONG_HEALING(4, Potions.STRONG_HEALING),
        REGENERATION(5, Potions.LONG_REGENERATION);

        private final int requiredExpertiseLevel;
        private final Holder<Potion> potion;

    }

}
