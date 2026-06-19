package dev.breezes.settlements.application.ai.dialogue;

import dev.breezes.settlements.application.ai.inference.monologue.VillagerFacetDeriver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Assembles the server-owned context for ambient and situational dialogue cues.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class AmbientDialogueContextAssembler {

    private final VillagerFacetDeriver facetDeriver;

    public DialogueContext assemble(@Nonnull BaseVillager villager) {
        return this.assemble(villager, villager.getCurrentOccasion());
    }

    public DialogueContext assemble(@Nonnull BaseVillager villager, @Nonnull Occasion occasion) {
        DialogueContext.DialogueContextBuilder builder = DialogueContext.builder()
                .profession(villager.getProfession())
                .occasion(occasion);

        // Delegate facet derivation to the shared deriver so MONOLOGUE and SCRIPTED
        // never drift on how cured-villager (or future) facets are detected.
        this.facetDeriver.derive(villager).forEach(builder::facet);

        return builder.build();
    }

}
