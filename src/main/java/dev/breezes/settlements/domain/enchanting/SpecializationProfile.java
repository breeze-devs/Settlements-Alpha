package dev.breezes.settlements.domain.enchanting;

import lombok.Builder;

import java.util.Map;

@Builder
public record SpecializationProfile(
        String id,
        String displayName,
        String description,
        Map<String, Float> enchantmentWeights
) {

    public SpecializationProfile {
        enchantmentWeights = Map.copyOf(enchantmentWeights == null ? Map.of() : enchantmentWeights);
    }

    public float getWeight(String enchantmentId) {
        return this.enchantmentWeights.getOrDefault(enchantmentId, 1.0f);
    }

}