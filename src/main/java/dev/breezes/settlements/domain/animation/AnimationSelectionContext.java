package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ItemCategory;

import javax.annotation.Nonnull;

public record AnimationSelectionContext(@Nonnull ItemCategory mainHandCategory) {

    private static final AnimationSelectionContext GENERIC = new AnimationSelectionContext(ItemCategory.GENERIC);

    public static AnimationSelectionContext generic() {
        return GENERIC;
    }

}
