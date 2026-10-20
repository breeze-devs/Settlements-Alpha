package dev.breezes.settlements.application.settlement.founding;

import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder.GeneratedSettlement;
import dev.breezes.settlements.domain.world.ChunkPosition;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The generated settlements a level's structure starts carry, whose records may not exist yet.
 */
public interface GeneratedSettlementStarts {

    /**
     * Returns the settlements carried by the generated settlement structure starts for the given chunk, regardless if they are recorded.
     * <p>
     * Must be called on the server thread. It may load the chunks those starts are saved in, from disk or by
     * generating them.
     */
    List<GeneratedSettlement> covering(@Nonnull ServerLevel level, @Nonnull ChunkPosition chunk);

}
