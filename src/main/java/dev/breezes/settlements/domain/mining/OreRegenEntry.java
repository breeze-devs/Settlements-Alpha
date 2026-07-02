package dev.breezes.settlements.domain.mining;

import com.mojang.serialization.Codec;
import lombok.Builder;
import lombok.Value;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * A single entry in the ore-regen weighted table.
 * Stores only the normalized runtime data so weighted-pool keys do not carry
 * deserialization-only fields in equality or hash calculations.
 */
@Value
@Builder
public class OreRegenEntry {

    /**
     * Fully-qualified block id, e.g. {@code minecraft:iron_ore}.
     */
    ResourceLocation blockId;

    /**
     * Relative roll weight — higher means more likely. Must be > 0.
     */
    double weight;

    /**
     * Parsed, normalized host filter. A blank/absent raw JSON host is normalized
     * to {@link HostFilter#ANY} before entries are cached.
     */
    HostFilter host;

    /**
     * Indicates which stratum this entry may recharge into.
     * Mirrors {@code DormantOreBlock.Host} but adds {@code ANY} so one global entry
     * can feed both block types without requiring two JSON files.
     */
    public enum HostFilter implements StringRepresentable {
        STONE("stone"),
        DEEPSLATE("deepslate"),
        ANY("any");

        public static final Codec<HostFilter> CODEC = StringRepresentable.fromEnum(HostFilter::values);

        private final String serializedName;

        HostFilter(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }

}
