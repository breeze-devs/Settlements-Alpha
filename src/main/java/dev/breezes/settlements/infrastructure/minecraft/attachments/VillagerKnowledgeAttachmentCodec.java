package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import dev.breezes.settlements.domain.ai.observation.ObservationType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mojang Codec for serializing {@link VillagerKnowledgeAttachmentState} to/from NBT.
 * Mirrors the style of {@link VillagerGeneticsAttachmentCodec}.
 * <p>
 * Nullable UUIDs (relatedEntity, source) and nullable KnowledgeResolution are encoded as absent
 * optional fields so the codec round-trips cleanly through NBT.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerKnowledgeAttachmentCodec {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    private static final Codec<ObservationType> OBSERVATION_TYPE_CODEC = enumCodec(ObservationType.class);

    private static final Codec<KnowledgeResolution> KNOWLEDGE_RESOLUTION_CODEC = enumCodec(KnowledgeResolution.class);

    private static final Codec<KnowledgeEntryState> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("originObservationId").forGetter(KnowledgeEntryState::originObservationId),
                    Codec.STRING.fieldOf("content").forGetter(KnowledgeEntryState::content),
                    OBSERVATION_TYPE_CODEC.fieldOf("type").forGetter(KnowledgeEntryState::type),
                    Codec.LONG.fieldOf("originTimestampTick").forGetter(KnowledgeEntryState::originTimestampTick),
                    Codec.LONG.fieldOf("admittedAtTick").forGetter(KnowledgeEntryState::admittedAtTick),
                    UUID_CODEC.optionalFieldOf("relatedEntity").forGetter(entry -> Optional.ofNullable(entry.relatedEntity())),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("metadata", Map.of()).forGetter(KnowledgeEntryState::metadata),
                    UUID_CODEC.optionalFieldOf("source").forGetter(entry -> Optional.ofNullable(entry.source())),
                    Codec.INT.fieldOf("hop").forGetter(KnowledgeEntryState::hop),
                    Codec.FLOAT.fieldOf("weight").forGetter(KnowledgeEntryState::weight),
                    Codec.FLOAT.fieldOf("originalWeight").forGetter(KnowledgeEntryState::originalWeight),
                    KNOWLEDGE_RESOLUTION_CODEC.optionalFieldOf("resolution").forGetter(entry -> Optional.ofNullable(entry.resolution())),
                    Codec.INT.optionalFieldOf("corroborationCount", 0).forGetter(KnowledgeEntryState::corroborationCount),
                    Codec.INT.optionalFieldOf("investigationAttempts", 0).forGetter(KnowledgeEntryState::investigationAttempts),
                    Codec.LONG.optionalFieldOf("nextEligibleTick", 0L).forGetter(KnowledgeEntryState::nextEligibleTick)
            ).apply(instance, VillagerKnowledgeAttachmentCodec::entryState));

    public static final Codec<VillagerKnowledgeAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.BOOL.optionalFieldOf("initialized", false).forGetter(VillagerKnowledgeAttachmentState::initialized),
                    ENTRY_CODEC.listOf().lenientOptionalFieldOf("entries", List.of())
                            .forGetter(VillagerKnowledgeAttachmentState::entries)
            ).apply(instance, VillagerKnowledgeAttachmentState::new));

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> enumClass) {
        return Codec.STRING.comapFlatMap(
                value -> parseEnum(enumClass, value),
                Enum::name);
    }

    private static <E extends Enum<E>> DataResult<E> parseEnum(Class<E> enumClass, String value) {
        try {
            return DataResult.success(Enum.valueOf(enumClass, value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown " + enumClass.getSimpleName() + " value: " + value);
        }
    }

    private static KnowledgeEntryState entryState(UUID originObservationId,
                                                  String content,
                                                  ObservationType type,
                                                  long originTimestampTick,
                                                  long admittedAtTick,
                                                  Optional<UUID> relatedEntity,
                                                  Map<String, String> metadata,
                                                  Optional<UUID> source,
                                                  int hop,
                                                  float weight,
                                                  float originalWeight,
                                                  Optional<KnowledgeResolution> resolution,
                                                  int corroborationCount,
                                                  int investigationAttempts,
                                                  long nextEligibleTick) {
        return KnowledgeEntryState.builder()
                .originObservationId(originObservationId)
                .content(content)
                .type(type)
                .originTimestampTick(originTimestampTick)
                .admittedAtTick(admittedAtTick)
                .relatedEntity(relatedEntity.orElse(null))
                .metadata(KnowledgeMetadataSanitizer.sanitize(metadata))
                .source(source.orElse(null))
                .hop(hop)
                .weight(weight)
                .originalWeight(originalWeight)
                .resolution(resolution.orElse(null))
                .corroborationCount(corroborationCount)
                .investigationAttempts(investigationAttempts)
                .nextEligibleTick(nextEligibleTick)
                .build();
    }

}
