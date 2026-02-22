package dev.breezes.settlements.domain.entities;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.infrastructure.rendering.bubbles.BubbleManager;
import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.world.entity.Entity;

public interface SettlementsEntity {

    Entity getMinecraftEntity();

    @ClientSide
    BubbleManager getBubbleManager();

    int getNetworkingId();

    default Location getLocation(boolean useEyeHeight) {
        return Location.fromEntity(this.getMinecraftEntity(), useEyeHeight);
    }

    default Location getLocation() {
        return getLocation(true);
    }

}
