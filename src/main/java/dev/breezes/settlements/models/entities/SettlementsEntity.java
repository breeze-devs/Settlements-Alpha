package dev.breezes.settlements.models.entities;

import dev.breezes.settlements.annotations.functional.ClientSide;
import dev.breezes.settlements.bubbles.BubbleManager;
import dev.breezes.settlements.models.location.Location;
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
