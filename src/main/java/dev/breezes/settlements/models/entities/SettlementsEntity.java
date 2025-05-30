package dev.breezes.settlements.models.entities;

import dev.breezes.settlements.annotations.functional.ClientSide;
import dev.breezes.settlements.bubbles.BubbleManager;
import net.minecraft.world.entity.Entity;

public interface SettlementsEntity {

    Entity getMinecraftEntity();

    @ClientSide
    BubbleManager getBubbleManager();

    int getNetworkingId();

}
