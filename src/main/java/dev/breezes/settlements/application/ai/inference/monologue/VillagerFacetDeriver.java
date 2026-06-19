package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.dialogue.DialogueFacet;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerWasCuredAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Derives the low-cardinality {@link DialogueFacet} set for a villager from durable attachment state.
 * <p>
 * Extracted here so both {@link MonologueRequestAssembler} (MONOLOGUE requests) and
 * {@code AmbientDialogueContextAssembler} (SCRIPTED context) read from exactly one source —
 * avoiding independent drift if new facets are added.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class VillagerFacetDeriver {

    public List<DialogueFacet> derive(@Nonnull BaseVillager villager) {
        List<DialogueFacet> facets = new ArrayList<>();
        if (VillagerWasCuredAttachment.wasCured(villager)) {
            facets.add(DialogueFacet.WAS_CURED);
        }
        return facets;
    }

}
