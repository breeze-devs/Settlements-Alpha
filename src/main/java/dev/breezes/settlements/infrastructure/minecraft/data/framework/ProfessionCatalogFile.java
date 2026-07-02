package dev.breezes.settlements.infrastructure.minecraft.data.framework;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.domain.entities.VillagerProfessionKeyCodec;

import java.util.List;

/**
 * Generic shape shared by every datapack file under a {@link ProfessionCatalogDataManager}
 * directory: one profession plus a list of profession-scoped values (recipes, stock policies, etc).
 */
public record ProfessionCatalogFile<T>(VillagerProfessionKey profession, List<T> values) {

    public static <V> Codec<ProfessionCatalogFile<V>> codec(Codec<V> valueCodec, String valuesField) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        VillagerProfessionKeyCodec.CODEC.fieldOf("profession").forGetter(ProfessionCatalogFile::profession),
                        valueCodec.listOf().fieldOf(valuesField).forGetter(ProfessionCatalogFile::values)
                ).apply(instance, ProfessionCatalogFile::new));
    }

}
