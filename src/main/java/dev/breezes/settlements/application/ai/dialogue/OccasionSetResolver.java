package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

import java.util.Set;

/**
 * Resolves the set of occasions for which a villager needs rehearsed packs.
 * <p>
 * The CORE set — the occasions every non-nitwit rehearses — is {@code IDLE, WORK, MEET, REST_DAY}.
 * Nitwits drop {@code WORK} (they have no work window, so a WORK pack would never be sampled),
 * keeping {@code IDLE, MEET, REST_DAY}.
 * <p>
 * {@code MORNING} and {@code EVENING} are intentionally excluded: in v1 they stay on the SCRIPTED
 * floor and are never rehearsed. {@code ZOMBIE_SIGHTED} is likewise excluded — it is a reactive,
 * in-the-moment cue, so generating a pack for it in the evening sweep would be both wasteful and
 * semantically wrong.
 */
public final class OccasionSetResolver {

    /**
     * Occasions all non-nitwit villagers rehearse.
     */
    private static final Set<Occasion> CORE_OCCASIONS = Set.of(
            Occasion.IDLE,
            Occasion.WORK,
            Occasion.MEET,
            Occasion.REST_DAY
    );

    /**
     * Occasions nitwits rehearse — WORK is excluded because nitwits have no work window,
     * so any WORK pack generated for them would never be sampled, wasting token budget.
     */
    private static final Set<Occasion> NITWIT_OCCASIONS = Set.of(
            Occasion.IDLE,
            Occasion.MEET,
            Occasion.REST_DAY
    );

    /**
     * Returns the set of occasions this villager should have rehearsed packs for.
     * The caller should not mutate the returned set.
     */
    public Set<Occasion> resolve(VillagerProfessionKey profession) {
        if (VillagerProfessionKey.NITWIT.equals(profession)) {
            return NITWIT_OCCASIONS;
        }
        return CORE_OCCASIONS;
    }

}
