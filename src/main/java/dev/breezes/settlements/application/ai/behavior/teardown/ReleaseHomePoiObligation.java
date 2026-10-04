package dev.breezes.settlements.application.ai.behavior.teardown;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

import javax.annotation.Nonnull;

/**
 * Releases a previously claimed HOME POI ticket when the behavior ends without handing it to a child.
 * <p>
 * Non-durable, which is safe only while the bed is claimed and then handed off or released within one tick, so no
 * save can fall between them. Ticket counts are saved with the world, so a claimed ticket saved before its handoff or
 * release would stay held, remembered by nobody, until its bed is broken.
 */
public record ReleaseHomePoiObligation(@Nonnull BlockPos bedPos) implements TeardownObligation {

    @Override
    public BlockPos targetPos() {
        return this.bedPos;
    }

    @Override
    public boolean stillValid(@Nonnull ServerLevel level) {
        if (!level.isLoaded(this.bedPos)) {
            return false;
        }
        // Only worth discharging if the POI is still a HOME; if it was removed, ticket already gone.
        return level.getPoiManager().exists(this.bedPos, holder -> holder.is(PoiTypes.HOME));
    }

    @Override
    public void discharge(@Nonnull ServerLevel level) {
        level.getPoiManager().release(this.bedPos);
        DebugPackets.sendPoiTicketCountPacket(level, this.bedPos);
    }

    @Override
    public boolean durable() {
        return false;
    }

    @Override
    public String describe() {
        return "release-home-poi@" + this.bedPos;
    }

}
