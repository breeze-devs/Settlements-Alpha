package dev.breezes.settlements.domain.ai.conditions;

import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.CustomLog;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Scans for entities that a nitwit can pelt with eggs — vanilla Villagers, BaseVillagers,
 * Players (non-spectator, non-creative), and WanderingTraders.
 * <p>
 * The result is cached in {@code targets} between calls to {@link #test}; callers that need
 * a fresh scan must call {@code test} again before reading {@code getTargets()}.
 */
@CustomLog
public class NearbyEggTargetCondition<T extends BaseVillager> implements IEntityCondition<T> {

    private static final Set<Class<?>> TARGET_CLASSES = Set.of(
            Villager.class,
            WanderingTrader.class,
            Player.class
    );

    private final double rangeHorizontal;
    private final double rangeVertical;

    /**
     * -- GETTER --
     * Returns the cached target list from the last
     * call; never null.
     */
    // Stored as LivingEntity rather than T because Player/WanderingTrader are valid targets
    // but do not extend T (BaseVillager). Callers consume this list as LivingEntity.
    @Getter
    private List<LivingEntity> targets;

    public NearbyEggTargetCondition(double rangeHorizontal, double rangeVertical) {
        this.rangeHorizontal = rangeHorizontal;
        this.rangeVertical = rangeVertical;
        this.targets = Collections.emptyList();
    }

    @Override
    public boolean test(@Nullable T villager) {
        if (villager == null) {
            log.sensorWarn("Source entity is null, returning empty targets");
            this.targets = Collections.emptyList();
            return false;
        }

        List<LivingEntity> found = new ArrayList<>();
        Location location = Location.fromEntity(villager, false);

        location.getNearbyEntities(rangeHorizontal, rangeVertical, rangeHorizontal, null, entity -> {
            if (entity == null || !entity.isAlive()) {
                return false;
            }
            // Never target ourselves
            if (entity == villager) {
                return false;
            }
            if (TARGET_CLASSES.stream().noneMatch(cls -> cls.isInstance(entity))) {
                return false;
            }
            // Skip spectator and creative players — they are not real participants in the world
            if (entity instanceof Player player && (player.isSpectator() || player.isCreative())) {
                return false;
            }
            if (!(entity instanceof LivingEntity livingEntity)) {
                return false;
            }
            found.add(livingEntity);
            return true;
        });

        this.targets = Collections.unmodifiableList(found);
        log.sensorStatus("Found {} egg targets nearby", this.targets.size());
        return !this.targets.isEmpty();
    }

}
