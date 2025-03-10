package dev.breezes.settlements.models.behaviors.states.registry.targets;

import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.util.SettlementsException;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.Entity;

@AllArgsConstructor
public class TargetableEntity implements Targetable {

    private final Entity target;

    @Override
    public TargetableType getType() {
        return TargetableType.ENTITY;
    }

    @Override
    public Entity getAsEntity() {
        return this.target;
    }

    @Override
    public PhysicalBlock getAsBlock() {
        throw new SettlementsException("Cannot get targetable entity as a block");
    }

    @Override
    public Location getLocation() {
        return Location.fromEntity(this.target, false);
    }

}
