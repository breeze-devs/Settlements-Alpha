package dev.breezes.settlements.domain.crafting.catalog;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A single generic crafting-grid recipe: a set of inputs consumed to produce one {@link CraftOutput}.
 * <p>
 * {@code priority} breaks selection ties when several recipes are equally attractive economically.
 */
@Builder
public record CraftRecipe(
        @Nonnull String id,
        @Nonnull List<CraftIngredient> inputs,
        @Nonnull CraftOutput output,
        int priority
) {

    public CraftRecipe {
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("Craft recipe id must not be blank");
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Craft recipe '" + id + "' must declare at least one input");
        }
    }

}
