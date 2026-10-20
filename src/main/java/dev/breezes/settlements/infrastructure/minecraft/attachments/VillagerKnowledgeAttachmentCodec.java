package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.shared.codec.ListCodecs;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Mojang Codec for serializing a villager's knowledge entries to/from NBT.
 * Mirrors the style of {@link VillagerGeneticsAttachmentCodec}.
 * <p>
 * Values that are absent on the entry — relatedEntity, pos — are encoded as absent optional fields
 * rather than as placeholders, so a round trip through NBT cannot invent one.
 * <p>
 * The entry's rendered content and semantic type carry no field of their own: both are functions
 * of the persisted metadata map, and a stored copy could contradict it.
 * <p>
 * A single unreadable entry costs only that entry. Episodic memory is lossy by design, so losing one
 * corrupted fact is survivable where losing a villager's entire history to it is not.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    private static final Codec<KnowledgeEntryState> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    UUID_CODEC.fieldOf("originObservationId").forGetter(KnowledgeEntryState::originObservationId),
                    Codec.LONG.fieldOf("originTimestampTick").forGetter(KnowledgeEntryState::originTimestampTick),
                    // Explicit Optional (not optionalFieldOf(name, default)) so encoding always omits
                    // the default value rather than depending on codec-version write behavior.
                    Codec.LONG.optionalFieldOf("admittedAtTick").forGetter(
                            entry -> entry.admittedAtTick() == entry.originTimestampTick()
                                    ? Optional.empty()
                                    : Optional.of(entry.admittedAtTick())),
                    UUID_CODEC.optionalFieldOf("relatedEntity").forGetter(entry -> Optional.ofNullable(entry.relatedEntity())),
                    Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("metadata", Map.of()).forGetter(KnowledgeEntryState::metadata),
                    Codec.LONG.optionalFieldOf("pos").forGetter(entry -> Optional.ofNullable(entry.packedPos())),
                    Codec.FLOAT.fieldOf("weight").forGetter(KnowledgeEntryState::weight)
            ).apply(instance, VillagerKnowledgeAttachmentCodec::entryState));

    public static final Codec<List<KnowledgeEntryState>> STATE_CODEC =
            ListCodecs.lenient(ENTRY_CODEC, "villager knowledge entry").optionalFieldOf("entries", List.of()).codec();

    private static KnowledgeEntryState entryState(UUID originObservationId,
                                                  long originTimestampTick,
                                                  Optional<Long> admittedAtTick,
                                                  Optional<UUID> relatedEntity,
                                                  Map<String, String> metadata,
                                                  Optional<Long> packedPos,
                                                  float weight) {
        return KnowledgeEntryState.builder()
                .originObservationId(originObservationId)
                .originTimestampTick(originTimestampTick)
                .admittedAtTick(admittedAtTick.orElse(originTimestampTick))
                .relatedEntity(relatedEntity.orElse(null))
                .metadata(KnowledgeMetadataSanitizer.sanitize(metadata))
                .packedPos(packedPos.orElse(null))
                .weight(weight)
                .build();
    }

}
