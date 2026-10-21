package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.breezes.settlements.domain.genetics.Gene;
import dev.breezes.settlements.domain.genetics.GeneType;
import dev.breezes.settlements.domain.genetics.GeneticsProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VillagerGeneticsAttachmentSerializerTest {

    private final VillagerGeneticsAttachmentSerializer serializer = new VillagerGeneticsAttachmentSerializer();

    @Test
    void roundTrip_preservesEveryGeneValue() {
        // Arrange — a distinct value per gene, so a swapped or dropped gene cannot pass
        Map<GeneType, Gene> genes = new EnumMap<>(GeneType.class);
        for (GeneType type : GeneType.VALUES) {
            genes.put(type, new Gene((type.ordinal() + 1) / (GeneType.VALUES.length + 1.0D)));
        }
        GeneticsProfile saved = new GeneticsProfile(genes);

        // Act
        JsonElement payload = VillagerGeneticsAttachmentCodec.STATE_CODEC
                .encodeStart(JsonOps.INSTANCE, this.serializer.toState(saved))
                .getOrThrow();
        List<VillagerGeneState> decoded = VillagerGeneticsAttachmentCodec.STATE_CODEC
                .parse(JsonOps.INSTANCE, payload)
                .resultOrPartial(Assertions::fail)
                .orElseThrow();
        GeneticsProfile loaded = this.serializer.fromState(decoded);

        // Assert
        for (GeneType type : GeneType.VALUES) {
            assertEquals(saved.getGeneValue(type), loaded.getGeneValue(type), type.name());
        }
    }

    @Test
    void fromState_fillsGenesTheSaveDoesNotCarry() {
        // Arrange — a save that predates every gene but one
        double savedStrength = 0.9D;
        List<VillagerGeneState> saved = List.of(new VillagerGeneState(GeneType.STRENGTH, savedStrength));

        // Act
        GeneticsProfile loaded = this.serializer.fromState(saved);

        // Assert — the missing genes hold values rather than reading at the profile's fallback
        assertEquals(EnumSet.allOf(GeneType.class), EnumSet.copyOf(loaded.getAllGenes().keySet()));
        assertEquals(savedStrength, loaded.getGeneValue(GeneType.STRENGTH));
    }

}
