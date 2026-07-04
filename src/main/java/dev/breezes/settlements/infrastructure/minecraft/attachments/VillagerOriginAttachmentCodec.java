package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.personality.OriginType;
import dev.breezes.settlements.shared.codec.EnumCodecs;

public final class VillagerOriginAttachmentCodec {

    private static final Codec<OriginType> ORIGIN_TYPE_CODEC = EnumCodecs.lenient(OriginType.class, OriginType.UNKNOWN);

    public static final Codec<VillagerOriginAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerOriginAttachmentState::initialized),
                    ORIGIN_TYPE_CODEC.optionalFieldOf("origin", OriginType.UNKNOWN).forGetter(VillagerOriginAttachmentState::origin)
            ).apply(instance, VillagerOriginAttachmentState::new));

}
