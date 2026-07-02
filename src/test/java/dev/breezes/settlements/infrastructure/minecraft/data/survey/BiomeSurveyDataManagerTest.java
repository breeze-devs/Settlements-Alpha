package dev.breezes.settlements.infrastructure.minecraft.data.survey;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.common.BiomeId;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeSurveyDataManagerTest {

    @Test
    void parsesTemplateTagsWhenPresent() {
        BiomeSurveyDataManager manager = new BiomeSurveyDataManager();
        manager.reload(Map.of(
                ResourceLocation.parse("settlements:biomes/survey/minecraft/taiga"),
                json("""
                        {
                          "biome": "minecraft:taiga",
                          "resource_densities": {"LUMBER": 0.6},
                          "template_tags": ["taiga", "cold"]
                        }
                        """)
        ));

        assertEquals(Set.of("taiga", "cold"), manager.lookup(BiomeId.of("minecraft:taiga")).templateTags());
    }

    @Test
    void missingSurveyUsesDefaultEmptyTemplateTags() {
        BiomeSurveyDataManager manager = new BiomeSurveyDataManager();
        manager.reload(Map.of());

        assertTrue(manager.lookup(BiomeId.of("minecraft:not_real")).templateTags().isEmpty());
    }

    private static JsonElement json(String raw) {
        return JsonParser.parseString(raw);
    }

}
