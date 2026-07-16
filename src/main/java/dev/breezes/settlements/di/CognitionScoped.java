package dev.breezes.settlements.di;

import javax.inject.Qualifier;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Dagger qualifier for multibinding contributions that only make sense while the SIS kill-switch
 * ({@code InferenceGate#isEnabled()}) is on.
 * <p>
 * Paired with {@link BaseLane} contributions and merged into a single
 * unqualified effective set by a gate-aware {@code @Provides} method in the owning module; every
 * consumer injects that unqualified set and never sees the split.
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CognitionScoped {
}
