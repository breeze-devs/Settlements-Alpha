package dev.breezes.settlements.domain.generation.model.building;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

public record DisplayInfo(
        String displayName,
        String description,
        @Nullable String customName,
        String iconItemId
) {

    public DisplayInfo {
        if (StringUtils.isBlank(displayName)) {
            throw new IllegalArgumentException("DisplayInfo displayName must not be blank");
        }
        if (StringUtils.isBlank(description)) {
            throw new IllegalArgumentException("DisplayInfo description must not be blank");
        }
        if (StringUtils.isBlank(iconItemId)) {
            throw new IllegalArgumentException("DisplayInfo iconItemId must not be blank");
        }
    }

}
