package dev.breezes.settlements.domain.crafting.catalog;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A single generic crafting-grid recipe: a set of inputs consumed to produce one {@link CraftOutput}.
 * <p>
 * Emeralds are paid from the crafter's wallet on each craft, alongside the item inputs; 0 means the recipe costs no
 * emeralds.
 * <p>
 * Priority breaks selection ties when several recipes are equally attractive economically.
 */
@Builder
public record CraftRecipe(
        @Nonnull String id,
        @Nonnull List<CraftIngredient> inputs,
        int emeralds,
        @Nonnull CraftOutput output,
        int priority
) {

    public CraftRecipe {
        Validate.isTrue(StringUtils.isNotBlank(id), "Craft recipe id must not be blank");
        Validate.notEmpty(inputs, "Craft recipe '%s' must declare at least one input", id);
        Validate.isTrue(emeralds >= 0, "Craft recipe '%s' must not cost a negative number of emeralds", id);
    }

}
