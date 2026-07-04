package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.ai.knowledge.KnowledgeResolution;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Mojang Codec for serializing {@link VillagerKnowledgeAttachmentState} to/from NBT.
 * Mirrors the style of {@link VillagerGeneticsAttachmentCodec}.
 * <p>
 * Nullable UUIDs (relatedEntity, source) and nullable KnowledgeResolution are encoded as absent
 * optional fields so the codec round-trips cleanly through NBT.
 * <p>
 * {@code content}, {@code type}, and {@code weight} are intentionally NOT persisted — they are
 * pure derivations reconstructed by {@link VillagerKnowledgeAttachment#loadInto} from fields that
 * ARE persisted here ({@code metadata}, {@code originalWeight}, {@code corroborationCount}).
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerKnowledgeAttachmentCodec {

    /**
     * Encodes a UUID as four ints (most-significant-bits high/low, least-significant-bits
     * high/low) instead of a 36-character string, matching vanilla's {@code UUIDUtil.CODEC} layout.
     */
    private static final Codec<UUID> UUID_CODEC = Codec.INT_STREAM.comapFlatMap(
            stream -> {
                int[] ints = stream.toArray();
                if (ints.length != 4) {
                    return DataResult.error(() -> "Expected 4 ints for a UUID, got " + ints.length);
                }
                long mostSigBits = (long) ints[0] << 32 | (ints[1] & 0xFFFFFFFFL);
                long leastSigBits = (long) ints[2] << 32 | (ints[3] & 0xFFFFFFFFL);
                return DataResult.success(new UUID(mostSigBits, leastSigBits));
            },
            uuid -> IntStream.of(
                    (int) (uuid.getMostSignificantBits() >> 32),
                    (int) uuid.getMostSignificantBits(),
                    (int) (uuid.getLeastSignificantBits() >> 32),
                    (int) uuid.getLeastSignificantBits()));

    private static final Codec<KnowledgeResolution> KNOWLEDGE_RESOLUTION_CODEC = enumCodec(KnowledgeResolution.class);

    private static final Codec<KnowledgeEntryState> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("originObservationId").forGetter(KnowledgeEntryState::originObservationId),
                    Codec.LONG.fieldOf("originTimestampTick").forGetter(KnowledgeEntryState::originTimestampTick),
                    Codec.LONG.optionalFieldOf("admittedAtTick").forGetter(
                            entry -> entry.admittedAtTick() == entry.originTimestampTick()
                                    ? Optional.empty()
                                    : Optional.of(entry.admittedAtTick())),
                    UUID_CODEC.optionalFieldOf("relatedEntity").forGetter(entry -> Optional.ofNullable(entry.relatedEntity())),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("metadata", Map.of()).forGetter(KnowledgeEntryState::metadata),
                    Codec.LONG.optionalFieldOf("pos").forGetter(entry -> Optional.ofNullable(entry.packedPos())),
                    UUID_CODEC.optionalFieldOf("source").forGetter(entry -> Optional.ofNullable(entry.source())),
                    // Explicit Optional (not optionalFieldOf(name, default)) so encoding always omits
                    // the default value rather than depending on codec-version write behavior.
                    Codec.INT.optionalFieldOf("hop").forGetter(
                            entry -> entry.hop() == 0 ? Optional.empty() : Optional.of(entry.hop())),
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
                                                  long originTimestampTick,
                                                  Optional<Long> admittedAtTick,
                                                  Optional<UUID> relatedEntity,
                                                  Map<String, String> metadata,
                                                  Optional<Long> packedPos,
                                                  Optional<UUID> source,
                                                  Optional<Integer> hop,
                                                  float originalWeight,
                                                  Optional<KnowledgeResolution> resolution,
                                                  int corroborationCount,
                                                  int investigationAttempts,
                                                  long nextEligibleTick) {
        return KnowledgeEntryState.builder()
                .originObservationId(originObservationId)
                .originTimestampTick(originTimestampTick)
                .admittedAtTick(admittedAtTick.orElse(originTimestampTick))
                .relatedEntity(relatedEntity.orElse(null))
                .metadata(KnowledgeMetadataSanitizer.sanitize(metadata))
                .packedPos(packedPos.orElse(null))
                .source(source.orElse(null))
                .hop(hop.orElse(0))
                .originalWeight(originalWeight)
                .resolution(resolution.orElse(null))
                .corroborationCount(corroborationCount)
                .investigationAttempts(investigationAttempts)
                .nextEligibleTick(nextEligibleTick)
                .build();
    }

}
