package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.UUID;

/**
 * Mojang Codec for serializing {@link VillagerCredibilityAttachmentState} to/from NBT.
 * Mirrors the style of {@link VillagerGeneticsAttachmentCodec}.
 */
public final class VillagerCredibilityAttachmentCodec {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<CredibilityScoreState> SCORE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("sourceId").forGetter(CredibilityScoreState::sourceId),
                    Codec.FLOAT.fieldOf("score").forGetter(CredibilityScoreState::score)
            ).apply(instance, CredibilityScoreState::new));

    public static final Codec<VillagerCredibilityAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerCredibilityAttachmentState::initialized),
                    SCORE_CODEC.listOf().optionalFieldOf("scores", List.of()).forGetter(VillagerCredibilityAttachmentState::scores)
            ).apply(instance, VillagerCredibilityAttachmentState::new));

}
