package dev.breezes.settlements.models.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
public enum BlockFlag {

    SEND_BLOCK_UPDATE(1),
    SEND_CLIENT_UPDATE(2),

    // TODO: NOTE: the following flags may be outdated
    PREVENT_RE_RENDER(4),
    FORCE_RE_RENDER_ON_MAIN_THREAD(8),
    PREVENT_OBSERVER_UPDATE(16),
    ;

    private final int flag;

    public static int of(@Nonnull BlockFlag... flags) {
        return Stream.of(flags)
                .mapToInt(BlockFlag::getFlag)
                .sum();
    }

}
