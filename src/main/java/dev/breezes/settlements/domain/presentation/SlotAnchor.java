package dev.breezes.settlements.domain.presentation;

import lombok.Builder;
import lombok.Getter;
import net.minecraft.world.item.ItemDisplayContext;

import javax.annotation.Nullable;

@Getter
@Builder
public final class SlotAnchor {

    private final SocketId socket;
    private final ItemDisplayContext defaultDisplayContext;
    @Nullable
    private final SocketId straightSocket;

    /**
     * Resolves which socket carries the held item based on the current arm pose.
     * Keeping the fallback to the default socket ensures that anchors without a straight
     * socket (e.g. IDENTITY) never produce a null lookup downstream.
     */
    public SocketId socketFor(ArmPose armPose) {
        if (armPose == ArmPose.STRAIGHT && this.straightSocket != null) {
            return this.straightSocket;
        }
        return this.socket;
    }

}
