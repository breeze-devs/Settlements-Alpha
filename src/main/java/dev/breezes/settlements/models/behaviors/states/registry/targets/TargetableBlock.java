package dev.breezes.settlements.models.behaviors.states.registry.targets;

import dev.breezes.settlements.models.blocks.PhysicalBlock;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.util.SettlementsException;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.Entity;

@AllArgsConstructor
public class TargetableBlock implements Targetable {

    private final PhysicalBlock target;

    @Override
    public TargetableType getType() {
        return TargetableType.BLOCK;
    }

    @Override
    public Entity getAsEntity() {
        throw new SettlementsException("Cannot get targetable block as an entity");
    }

    @Override
    public PhysicalBlock getAsBlock() {
        return this.target;
    }

    @Override
    public Location getLocation() {
        return this.target.getLocation(true);
    }

}
