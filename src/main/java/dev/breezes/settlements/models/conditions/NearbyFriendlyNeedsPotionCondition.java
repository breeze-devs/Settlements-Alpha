package dev.breezes.settlements.models.conditions;

import com.google.common.base.Predicates;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.location.Location;
import lombok.AllArgsConstructor;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.horse.TraderLlama;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * Condition for clerics to find a friendly entity that's in need of a potion
 */
@CustomLog
public class NearbyFriendlyNeedsPotionCondition<T extends BaseVillager> implements IEntityCondition<T> {

    private static final Set<Class<?>> FRIENDLY_ENTITY_CLASSES = Set.of(Player.class, Villager.class, WanderingTrader.class, TraderLlama.class, IronGolem.class, SnowGolem.class);

    private final double rangeHorizontal;
    private final double rangeVertical;
    private final int minimumReputation;

    @Getter
    private Map<Entity, PotionType> friendlyNeedsPotionMap;

    public NearbyFriendlyNeedsPotionCondition(double rangeHorizontal, double rangeVertical, int minimumReputation) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.minimumReputation = minimumReputation;

        this.friendlyNeedsPotionMap = Collections.emptyMap();
    }

    @Override
    public boolean test(@Nullable T villager) {
        if (villager == null) {
            log.sensorWarn("Source entity is null, returning empty targets");
            this.friendlyNeedsPotionMap = Collections.emptyMap();
            return false;
        }

        // Initialize map
        this.friendlyNeedsPotionMap = new HashMap<>();

        Location location = Location.fromEntity(villager, false);
        location.getNearbyEntities(rangeHorizontal, rangeVertical, rangeHorizontal, null, (entity) -> {
            if (entity == null || !entity.isAlive() || FRIENDLY_ENTITY_CLASSES.stream().noneMatch(type -> type.isInstance(entity))) {
                return false;
            }

            // Filter out players with bad reputation
            if (entity.getType() == EntityType.PLAYER) {
                int reputation = this.getReputation(villager, (Player) entity);
                if (reputation < this.minimumReputation) {
                    log.sensorTrace("Ignoring player '%s' with reputation '%d' below minimum reputation of '%d'", entity.getName(), reputation, this.minimumReputation);
                    return false;
                }
            }

            // Check if entity needs a potion
            Optional<PotionType> potionType = this.needsPotion(villager, entity);
            potionType.ifPresent(type -> this.friendlyNeedsPotionMap.put(entity, type));

            log.sensorTrace("Entity '%s' needs potion type: %s", entity.getName(), potionType.orElse(null));
            return potionType.isPresent();
        });

        log.sensorStatus("Found %d friendly entities that need a potion", this.friendlyNeedsPotionMap.size());
        return !this.friendlyNeedsPotionMap.isEmpty();
    }

    private int getReputation(@Nonnull BaseVillager villager, @Nonnull Player player) {
        return villager.getGossips().getReputation(player.getUUID(), Predicates.alwaysTrue());
    }

    private Optional<PotionType> needsPotion(@Nonnull BaseVillager villager, @Nonnull Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }

        int expertiseLevel = villager.getExpertise().getLevel();
        if (livingEntity.isUnderWater() && !livingEntity.hasEffect(MobEffects.WATER_BREATHING)
                && expertiseLevel >= PotionType.WATER_BREATHING.getRequiredExpertiseLevel()) {
            return Optional.of(PotionType.WATER_BREATHING);
        } else if (livingEntity.isOnFire() && !livingEntity.hasEffect(MobEffects.FIRE_RESISTANCE)
                && expertiseLevel >= PotionType.FIRE_RESISTANCE.getRequiredExpertiseLevel()) {
            return Optional.of(PotionType.FIRE_RESISTANCE);
        } else if (livingEntity.getHealth() < livingEntity.getMaxHealth()) {
            if (!livingEntity.hasEffect(MobEffects.REGENERATION)
                    && expertiseLevel >= PotionType.REGENERATION.getRequiredExpertiseLevel()) {
                return Optional.of(PotionType.REGENERATION);
            } else if (expertiseLevel >= PotionType.STRONG_HEALING.getRequiredExpertiseLevel()) {
                return Optional.of(PotionType.STRONG_HEALING);
            } else {
                return Optional.of(PotionType.HEALING);
            }
        }
        return Optional.empty();
    }

    @AllArgsConstructor
    @Getter
    public enum PotionType {

        HEALING(1, Potions.HEALING),
        WATER_BREATHING(2, Potions.WATER_BREATHING),
        FIRE_RESISTANCE(3, Potions.FIRE_RESISTANCE),
        STRONG_HEALING(4, Potions.STRONG_HEALING),
        REGENERATION(5, Potions.STRONG_REGENERATION);

        private final int requiredExpertiseLevel;
        private final Holder<Potion> potion;

    }

}
