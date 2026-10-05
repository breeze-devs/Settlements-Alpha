package dev.breezes.settlements.domain.entities;

import dev.breezes.settlements.domain.world.location.Location;
import net.minecraft.world.entity.Entity;

public interface SettlementsEntity {

    Entity getMinecraftEntity();

    int getNetworkingId();

    default Location getLocation(boolean useEyeHeight) {
        return Location.fromEntity(this.getMinecraftEntity(), useEyeHeight);
    }

    default Location getLocation() {
        return getLocation(true);
    }

}
