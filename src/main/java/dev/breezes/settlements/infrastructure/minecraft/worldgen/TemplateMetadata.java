package dev.breezes.settlements.infrastructure.minecraft.worldgen;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Companion metadata for a settlement NBT template.
 */
@Builder
@Getter
public final class TemplateMetadata {

    private final String id;

    @SerializedName("building_definition")
    private final String buildingDefinition;

    @Builder.Default
    private final Set<String> tags = Set.of();
    @Builder.Default
    private final List<String> authors = List.of();

    @SerializedName("width")
    private final int width;
    @SerializedName("depth")
    private final int depth;
    @SerializedName("height")
    private final int height;

    @SerializedName("min_minecraft_version")
    @Nullable
    private final String minMinecraftVersion;

    @Nullable
    private final String notes;

    /**
     * This value is derived from the discovered resource path rather than the JSON payload,
     * so it is attached after deserialization during catalog materialization.
     */
    @Setter
    @Nullable
    private ResourceLocation resourceLocation;

    public ResourceLocation resourceLocationOrThrow() {
        if (this.resourceLocation == null) {
            throw new IllegalStateException("TemplateMetadata.resourceLocation must be non-null after validation");
        }
        return this.resourceLocation;
    }

    public boolean isValid() {
        if (this.id == null || this.id.isBlank()) {
            return false;
        }
        if (this.buildingDefinition == null || this.buildingDefinition.isBlank()) {
            return false;
        }

        // Tags are normalized during loading, so validation here focuses on semantic
        // correctness of the resulting metadata rather than raw JSON presence.
        return this.width > 0 && this.depth > 0 && this.height > 0;
    }

}
