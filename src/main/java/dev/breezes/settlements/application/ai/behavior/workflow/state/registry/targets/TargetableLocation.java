package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.targets;

import dev.breezes.settlements.domain.world.blocks.PhysicalBlock;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.shared.util.SettlementsException;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.Entity;

@AllArgsConstructor
public class TargetableLocation implements Targetable {

    private final Location target;

    @Override
    public TargetableType getType() {
        return TargetableType.LOCATION;
    }

    @Override
    public Entity getAsEntity() {
        throw new SettlementsException("Cannot get targetable location as an entity");
    }

    @Override
    public PhysicalBlock getAsBlock() {
        throw new SettlementsException("Cannot get targetable location as a block");
    }

    @Override
    public Location getLocation() {
        // Hand back a copy so callers that mutate the returned location (e.g. add/center) cannot corrupt the target
        return this.target.clone();
    }

}
