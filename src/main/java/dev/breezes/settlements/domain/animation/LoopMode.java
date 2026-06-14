package dev.breezes.settlements.domain.animation;

public enum LoopMode {

    ONCE,
    LOOP,
    PING_PONG;

    public float resolveTick(float elapsedTicks, int durationTicks) {
        if (durationTicks <= 0) {
            return 0.0F;
        }

        float nonNegativeElapsed = Math.max(0.0F, elapsedTicks);
        return switch (this) {
            case ONCE -> Math.min(nonNegativeElapsed, durationTicks);
            case LOOP -> nonNegativeElapsed % durationTicks;
            case PING_PONG -> resolvePingPongTick(nonNegativeElapsed, durationTicks);
        };
    }

    private static float resolvePingPongTick(float elapsedTicks, int durationTicks) {
        float cycleLength = durationTicks * 2.0F;
        float cycleTick = elapsedTicks % cycleLength;
        if (cycleTick <= durationTicks) {
            return cycleTick;
        }
        return cycleLength - cycleTick;
    }

}
