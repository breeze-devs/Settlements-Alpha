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

    public static final Codec<VillagerGeneticsAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerGeneticsAttachmentState::initialized),
                    GENE_CODEC.listOf().optionalFieldOf("genes", List.of()).forGetter(VillagerGeneticsAttachmentState::genes)
            ).apply(instance, VillagerGeneticsAttachmentState::new));

}
