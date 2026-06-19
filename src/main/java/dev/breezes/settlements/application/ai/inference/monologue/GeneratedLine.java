package dev.breezes.settlements.application.ai.inference.monologue;

import lombok.Builder;
import lombok.Getter;

/**
 * Ready-to-render text returned by the MONOLOGUE capability
 */
@Builder
@Getter
public final class GeneratedLine {

    private final String text;

    @Builder.Default
    private final LineStatus status = LineStatus.OK;

}
