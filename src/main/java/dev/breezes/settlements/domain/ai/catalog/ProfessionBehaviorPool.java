package dev.breezes.settlements.domain.ai.catalog;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

/**
 * Maps a single profession to the set of behaviors it can perform.
 * <p>
 * This is the job description — a list of {@link PoolEntry} keys that reference behaviors
 * in {@link IBehaviorCatalog}. It answers: "what can this profession do?"
 * <p>
 * Universal behaviors (eat, wander, rest, trade) are handled separately by
 * {@code BehaviorPoolResolver} and are not repeated in each profession pool.
 * <p>
 * Pools are registered via Dagger multibinding in {@code PoolModule}. Adding a new profession
 * requires only a new pool definition here — no catalog changes needed.
 */
@Builder
@Getter
public class ProfessionBehaviorPool {

    private final VillagerProfessionKey profession;

    @Singular
    private final List<PoolEntry> entries;

    public boolean contains(BehaviorKey key) {
        return this.entries.stream().anyMatch(e -> e.key().equals(key));
    }

}
