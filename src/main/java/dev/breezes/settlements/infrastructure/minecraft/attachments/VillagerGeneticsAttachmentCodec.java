package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.genetics.GeneType;

import java.util.List;

public final class VillagerGeneticsAttachmentCodec {

    private static final Codec<GeneType> GENE_TYPE_CODEC = Codec.STRING.xmap(GeneType::valueOf, GeneType::name);

    private static final Codec<VillagerGeneState> GENE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    GENE_TYPE_CODEC.fieldOf("type").forGetter(VillagerGeneState::type),
                    Codec.DOUBLE.fieldOf("value").forGetter(VillagerGeneState::value)
            ).apply(instance, VillagerGeneState::new));

    public static final Codec<List<VillagerGeneState>> STATE_CODEC =
            GENE_CODEC.listOf().optionalFieldOf("genes", List.of()).codec();

}
