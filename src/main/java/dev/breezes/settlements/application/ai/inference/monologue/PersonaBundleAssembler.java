package dev.breezes.settlements.application.ai.inference.monologue;

import dev.breezes.settlements.application.ai.genetics.PersonalityDeriver;
import dev.breezes.settlements.application.ai.naming.VillagerNameResolver;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * Converts live villager state into structured persona tokens for backend-owned prompt construction.
 */
@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class PersonaBundleAssembler {

    private final PersonalityDeriver personalityDeriver;
    private final VillagerNameResolver nameResolver;

    public PersonaBundle assemble(@Nonnull BaseVillager villager) {
        return PersonaBundle.builder()
                .name(this.nameResolver.resolve(villager.getUUID()))
                .professionKey(villager.getProfession().id())
                .traits(this.personalityDeriver.derivePersonalityTraits(villager.getGenetics()))
                .build();
    }

}
