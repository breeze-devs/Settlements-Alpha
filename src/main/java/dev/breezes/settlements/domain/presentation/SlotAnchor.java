package dev.breezes.settlements.domain.presentation;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.item.ItemDisplayContext;

@Getter
@Builder
public final class SlotAnchor {

    private final SocketId socket;
    private final ItemDisplayContext defaultDisplayContext;

}
