package dev.breezes.settlements.domain.ai.memory;

import dev.breezes.settlements.domain.ai.brain.IBrain;

import javax.annotation.Nonnull;

/**
 * A pending write to an agent's memory. Applied via {@link IBrain#applyWrite(IMemoryWrite)},
 * which dispatches to the correct backing (vanilla brain or decaying store).
 * <p>
 * Two implementations exist:
 * <ul>
 *   <li>{@link MemoryWrite} — set/clear a value on a vanilla-backed memory.</li>
 *   <li>{@link ObservationUpdateWrite} — fold an observation report into a decaying spatial store.</li>
 * </ul>
 */
public interface IMemoryWrite {

    void applyTo(@Nonnull IBrain brain);

}
