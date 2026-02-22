package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets;

import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.shared.util.SettlementsException;
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
