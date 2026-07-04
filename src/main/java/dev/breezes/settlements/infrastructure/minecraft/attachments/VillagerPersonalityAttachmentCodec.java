package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.personality.PersonalityStatus;
import dev.breezes.settlements.domain.personality.VillagerPersonality;
import dev.breezes.settlements.shared.codec.EnumCodecs;

import java.util.List;

public final class VillagerPersonalityAttachmentCodec {

    private static final Codec<PersonalityStatus> STATUS_CODEC = EnumCodecs.lenient(PersonalityStatus.class, PersonalityStatus.PENDING);

    public static final Codec<VillagerPersonality> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    STATUS_CODEC.optionalFieldOf("status", PersonalityStatus.PENDING).forGetter(VillagerPersonality::status),
                    Codec.STRING.listOf().optionalFieldOf("adjectives", List.of()).forGetter(VillagerPersonality::adjectives),
                    Codec.STRING.optionalFieldOf("characterSketch", "").forGetter(VillagerPersonality::characterSketch),
                    Codec.STRING.optionalFieldOf("speechStyle", "").forGetter(VillagerPersonality::speechStyle)
            ).apply(instance, VillagerPersonality::new));

}
