package dev.breezes.settlements.application.ai.behavior.runtime;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class BehaviorLifecycleResult {

    public enum Kind {
        CLEAN,
        FAIL,
        ABORT
    }

    public static final BehaviorLifecycleResult CLEAN = new BehaviorLifecycleResult(Kind.CLEAN, null);

    private final Kind kind;
    @Nullable
    private final String reason;

    public static BehaviorLifecycleResult clean() {
        return CLEAN;
    }

    public static BehaviorLifecycleResult fail(@Nonnull String reason) {
        return new BehaviorLifecycleResult(Kind.FAIL, reason);
    }

    public static BehaviorLifecycleResult abort(@Nonnull String reason) {
        return new BehaviorLifecycleResult(Kind.ABORT, reason);
    }

    public boolean isClean() {
        return this.kind == Kind.CLEAN;
    }

}
