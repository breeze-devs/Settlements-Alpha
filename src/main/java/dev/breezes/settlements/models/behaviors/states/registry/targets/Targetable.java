package dev.breezes.settlements.models.behaviors.states.registry.targets;

import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.location.Location;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nonnull;

public interface Targetable {

    TargetableType getType();

    static Targetable fromEntity(@Nonnull Entity entity) {
        return new TargetableEntity(entity);
    }
    
    static Targetable fromBlock(@Nonnull PhysicalBlock block) {
        return new TargetableBlock(block);
    }

    /**
     * Get the target as an entity
     *
     * @throws dev.breezes.settlements.util.SettlementsException if the target is not an entity
     */
    Entity getAsEntity();

    /**
     * Get the target as a block
     *
     * @throws dev.breezes.settlements.util.SettlementsException if the target is not a block
     */
    PhysicalBlock getAsBlock();

    Location getLocation();

}
