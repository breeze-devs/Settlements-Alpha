package dev.breezes.settlements.infrastructure.network.features.ballista.handler;

import dev.breezes.settlements.infrastructure.minecraft.blocks.ballista.BallistaBlockEntity;
import dev.breezes.settlements.infrastructure.network.core.ServerSidePacketHandler;
import dev.breezes.settlements.infrastructure.network.features.ballista.packet.ServerBoundBallistaAimPacket;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nonnull;

/**
 * Validates player aim requests before applying them to a ballista.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public class ServerBoundBallistaAimPacketHandler implements ServerSidePacketHandler<ServerBoundBallistaAimPacket> {

    @Override
    public void runOnServer(@Nonnull IPayloadContext context, @Nonnull ServerBoundBallistaAimPacket packet) {
        if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) {
            return;
        }
        // Reject non-finite angles before they enter the ballista's state
        if (!Float.isFinite(packet.yaw()) || !Float.isFinite(packet.pitch())) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = packet.pos();

        // Check chunk availability first so an aim request cannot trigger a chunk load
        if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BallistaBlockEntity ballista)) {
            return;
        }

        if (BallistaBlockEntity.isOutsideOperatingRange(player.getEyePosition(), pos)) {
            // Correct the client's predicted aim with the server's target angles
            Packet<?> correction = ballista.getUpdatePacket();
            if (correction != null) {
                player.connection.send(correction);
            }
            return;
        }

        ballista.aimAt(packet.yaw(), packet.pitch());
    }

}
