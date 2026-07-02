package dev.breezes.settlements.infrastructure.minecraft.data.scoring;

import com.google.gson.JsonParser;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.generation.model.survey.ResourceTag;
import dev.breezes.settlements.domain.generation.scoring.ConfiguredTraitScorer;
import dev.breezes.settlements.domain.generation.scoring.TraitScorer;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraitScorerDataManagerTest {

    private static final TraitId MINING = TraitId.of("settlements:settlement_traits/mining");

    private final TraitScorerDataManager manager = new TraitScorerDataManager();

    @Test
    void valid_config_is_loaded_with_its_resource_tag_weights() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:traits/scoring/mining"), JsonParser.parseString("""
                        {
                          "trait": "settlements:settlement_traits/mining",
                          "base_score": 0.5,
                          "resource_tag_weights": {
                            "STONE": 0.4,
                            "ORE_BEARING": 0.35
                          }
                        }
                        """)
        ));

        // Assert
        TraitScorer scorer = this.manager.allScorers().get(MINING);
        assertTrue(scorer instanceof ConfiguredTraitScorer, "Mining scorer should be present");
        ConfiguredTraitScorer configured = (ConfiguredTraitScorer) scorer;
        assertEquals(0.5f, configured.config().baseScore());
        assertEquals(0.4f, configured.config().resourceTagWeights().get(ResourceTag.STONE));
    }

    @Test
    void unknown_resource_tag_key_rejects_whole_entry() {
        // Arrange & Act
        this.manager.reload(Map.of(
                resource("settlements:traits/scoring/broken"), JsonParser.parseString("""
                        {
                          "trait": "settlements:settlement_traits/mining",
                          "base_score": 0.5,
                          "resource_tag_weights": {
                            "NOT_A_TAG": 0.4
                          }
                        }
                        """)
        ));

        // Assert — an unknown resource tag anywhere in the entry fails the whole decode
        assertTrue(this.manager.allScorers().isEmpty(), "Entry with unknown resource tag must be rejected wholesale");
    }

    private static ResourceLocation resource(String value) {
        return ResourceLocation.parse(value);
    }

}
