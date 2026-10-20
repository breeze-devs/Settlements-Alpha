package dev.breezes.settlements.infrastructure.minecraft.persistence;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.entities.VillagerProfessionKeyCodec;
import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.generation.model.profile.DefenseLevel;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitIdCodec;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.settlement.model.SettlementRosterEntry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.shared.codec.EnumCodecs;
import dev.breezes.settlements.shared.codec.ListCodecs;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Codecs for a dimension's settlement store.
 * <p>
 * The stored form carries its format version. Decoding keeps valid settlement and tombstone entries and discarding
 * invalid ones. An invalid roster, log, history, gathering tally, or fading time falls back to its default instead
 * of dropping the settlement entry.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@CustomLog
public final class SettlementSavedDataCodec {

    /**
     * The format version this codec writes.
     * Raise it when the stored form changes in a way the previous reader would misread, and convert the older form on decode.
     */
    static final int FORMAT_VERSION = 1;

    /**
     * A settlement's claim: its center chunk's coordinates, and its chunks packed as vanilla packs chunk positions.
     */
    public static final Codec<SettlementBounds> BOUNDS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("center_chunk_x").forGetter(bounds -> bounds.center().x()),
            Codec.INT.fieldOf("center_chunk_z").forGetter(bounds -> bounds.center().z()),
            // Sorted so a claim is written the same way whatever order its set iterates in
            Codec.LONG_STREAM.fieldOf("chunks").forGetter(bounds -> bounds.chunks().stream()
                    .mapToLong(chunk -> ChunkPos.asLong(chunk.x(), chunk.z()))
                    .sorted())
    ).apply(instance, (centerX, centerZ, chunks) -> new SettlementBounds(new ChunkPosition(centerX, centerZ),
            chunks.mapToObj(chunk -> new ChunkPosition(ChunkPos.getX(chunk), ChunkPos.getZ(chunk)))
                    .collect(Collectors.toSet()))));

    private static final Codec<CivilDateTime> DATE_CODEC = Codec.LONG.xmap(CivilDateTime::new, CivilDateTime::dayTime);

    private static final Codec<SettlementFounding> FOUNDING_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DATE_CODEC.fieldOf("founded").forGetter(SettlementFounding::founded),
            DATE_CODEC.fieldOf("recorded").forGetter(SettlementFounding::recorded)
    ).apply(instance, SettlementFounding::new));

    private static final Codec<HistoryEvent> HISTORY_EVENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("event_id").forGetter(HistoryEvent::eventId),
            DATE_CODEC.fieldOf("day_time").forGetter(HistoryEvent::date)
    ).apply(instance, HistoryEvent::new));

    /**
     * A settlement's profile.
     */
    public static final Codec<SettlementProfile> PROFILE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TraitIdCodec.CODEC.fieldOf("primary").forGetter(SettlementProfile::primary),
            TraitIdCodec.CODEC.listOf().fieldOf("secondary").forGetter(SettlementProfile::secondary),
            TraitIdCodec.CODEC.listOf().fieldOf("flavor").forGetter(SettlementProfile::flavor),
            Codec.unboundedMap(TraitIdCodec.CODEC, Codec.FLOAT).fieldOf("adjusted_weights")
                    .forGetter(SettlementProfile::adjustedWeights),
            EnumCodecs.lenient(ScaleTier.class, ScaleTier.VILLAGE, "settlement scale tier").fieldOf("scale_tier")
                    .forGetter(SettlementProfile::scaleTier),
            Codec.INT.fieldOf("initial_population").forGetter(SettlementProfile::initialPopulation),
            Codec.FLOAT.fieldOf("wealth").forGetter(SettlementProfile::wealthLevel),
            EnumCodecs.lenient(DefenseLevel.class, DefenseLevel.NONE, "settlement defense level").fieldOf("defense")
                    .forGetter(SettlementProfile::defenseLevel),
            Codec.LONG.fieldOf("seed").forGetter(SettlementProfile::seed)
    ).apply(instance, SettlementProfile::new));

    private static final Codec<SettlementRosterEntry> ROSTER_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("member_id").forGetter(SettlementRosterEntry::memberId),
            VillagerProfessionKeyCodec.CODEC.fieldOf("profession").forGetter(SettlementRosterEntry::profession),
            Codec.BOOL.fieldOf("adult").forGetter(SettlementRosterEntry::adult),
            Codec.BOOL.fieldOf("housed").forGetter(SettlementRosterEntry::housed),
            Codec.INT.fieldOf("gathering_mark").forGetter(SettlementRosterEntry::gatheringCountAtLastCheckIn)
    ).apply(instance, SettlementRosterEntry::new));

    private static final Codec<SettlementRoster> ROSTER_CODEC =
            ListCodecs.lenient(ROSTER_ENTRY_CODEC, "settlement roster entry")
                    .xmap(SettlementSavedDataCodec::roster, SettlementRoster::entries);

    private static final Codec<SettlementLogEntry> LOG_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DATE_CODEC.fieldOf("day_time").forGetter(SettlementLogEntry::date),
            strictEnum(SettlementLogType.class, "settlement log type").fieldOf("type")
                    .forGetter(SettlementLogEntry::type),
            UUIDUtil.CODEC.optionalFieldOf("subject_id").forGetter(entry -> Optional.ofNullable(entry.subjectId())),
            Codec.STRING.optionalFieldOf("subject_name").forGetter(entry -> Optional.ofNullable(entry.subjectName())),
            Codec.STRING.optionalFieldOf("detail").forGetter(entry -> Optional.ofNullable(entry.detail()))
    ).apply(instance, (date, type, subjectId, subjectName, detail) -> SettlementLogEntry.builder()
            .date(date)
            .type(type)
            .subjectId(subjectId.orElse(null))
            .subjectName(subjectName.orElse(null))
            .detail(detail.orElse(null))
            .build()));

    private static final Codec<SettlementLog> LOG_CODEC = ListCodecs.lenient(LOG_ENTRY_CODEC, "settlement log entry")
            .xmap(SettlementLog::new, SettlementLog::entries);

    private static final Codec<SettlementGatherings> GATHERINGS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("count").forGetter(SettlementGatherings::count),
            Codec.LONG.optionalFieldOf("last_day").forGetter(gatherings -> gatherings.count() == 0
                    ? Optional.empty()
                    : Optional.of(gatherings.lastDay()))
    ).apply(instance, (count, lastDay) -> gatherings(count, lastDay.orElse(SettlementGatherings.NO_DAY))));

    // Rejected values fail the decode rather than throwing, so the fading field's fallback reads them as not fading
    private static final Codec<SettlementFading> FADING_CODEC = Codec.LONG.comapFlatMap(
            SettlementSavedDataCodec::fading, fading -> fading.elapsed().getTicks());

    private static final Codec<Settlement> SETTLEMENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Settlement::id),
            Codec.STRING.fieldOf("name").forGetter(Settlement::name),
            // Strict: no origin is a safe guess; a dropped settlement is recorded again from its structure start
            strictEnum(SettlementOrigin.class, "settlement origin").fieldOf("origin").forGetter(Settlement::origin),
            BOUNDS_CODEC.fieldOf("bounds").forGetter(Settlement::bounds),
            FOUNDING_CODEC.fieldOf("founding").forGetter(Settlement::founding),
            PROFILE_CODEC.fieldOf("profile").forGetter(Settlement::profile),
            // An optional field's unreadable value fails the whole record, so each field the settlement can do without
            // falls back to its default instead of costing the settlement its roster and log
            orDefault(ListCodecs.lenient(HISTORY_EVENT_CODEC, "settlement history event")
                    .optionalFieldOf("history", List.of()), "history", List.of())
                    .forGetter(Settlement::history),
            orDefault(ROSTER_CODEC.optionalFieldOf("roster", SettlementRoster.EMPTY), "roster", SettlementRoster.EMPTY)
                    .forGetter(Settlement::roster),
            orDefault(GATHERINGS_CODEC.optionalFieldOf("gatherings", SettlementGatherings.NONE), "gatherings",
                    SettlementGatherings.NONE)
                    .forGetter(Settlement::gatherings),
            orDefault(FADING_CODEC.optionalFieldOf("fading_ticks_elapsed"), "fading time",
                    Optional.empty())
                    .forGetter(settlement -> Optional.ofNullable(settlement.fading())),
            orDefault(LOG_CODEC.optionalFieldOf("log", SettlementLog.EMPTY), "log", SettlementLog.EMPTY)
                    .forGetter(Settlement::log)
    ).apply(instance, (id, name, origin, bounds, founding, profile, history, roster, gatherings, fading,
                       settlementLog) -> Settlement.builder()
            .id(id)
            .name(name)
            .origin(origin)
            .bounds(bounds)
            .founding(founding)
            .profile(profile)
            .history(history)
            .roster(roster)
            .gatherings(gatherings)
            .fading(fading.orElse(null))
            .log(settlementLog)
            .build()));

    // An object rather than the bare id, so a tombstone can gain fields without a format change
    private static final Codec<UUID> TOMBSTONE_CODEC = UUIDUtil.CODEC.fieldOf("id").codec();

    /**
     * A dimension's settlements and tombstones with the store's format version. Each settlement and tombstone decodes
     * on its own.
     */
    public static final Codec<StoredState> STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("format_version").forGetter(state -> FORMAT_VERSION),
            ListCodecs.lenient(SETTLEMENT_CODEC, "settlement").fieldOf("settlements")
                    .forGetter(StoredState::settlements),
            // Optional because a store without tombstones omits it, and falling back rather than failing because an
            // unreadable value here would otherwise cost the whole store its settlements
            orDefault(ListCodecs.lenient(TOMBSTONE_CODEC, "settlement tombstone")
                    .optionalFieldOf("tombstones", List.of()), "tombstones", List.of())
                    .forGetter(StoredState::tombstones)
    ).apply(instance, SettlementSavedDataCodec::readState));

    private static <A> MapCodec<A> orDefault(MapCodec<A> field, String name, A fallback) {
        // Not MapCodec.orElse: it maps the error lazily and then discards it unread, so its callback never runs and
        // the fallback would go unlogged. Reading the result's message here is what evaluates it.
        return field.mapResult(new MapCodec.ResultFunction<>() {
            @Override
            public <T> DataResult<A> apply(DynamicOps<T> ops, MapLike<T> input, DataResult<A> result) {
                return DataResult.success(result
                        .resultOrPartial(error -> log.warn("Reading an unreadable settlement {} as its default: {}", name, error))
                        .orElse(fallback));
            }

            @Override
            public <T> RecordBuilder<T> coApply(DynamicOps<T> ops, A input, RecordBuilder<T> builder) {
                return builder;
            }
        });
    }

    private static <E extends Enum<E>> Codec<E> strictEnum(Class<E> type, String label) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name));
            } catch (IllegalArgumentException exception) {
                return DataResult.error(() -> "Unknown " + label + " '" + name + "'");
            }
        }, Enum::name);
    }

    private static SettlementGatherings gatherings(int count, long lastDay) {
        // Gatherings are a tally that later gatherings rebuild, so a tally the settlement rejects is cleared rather
        // than costing the settlement its roster and log
        try {
            return new SettlementGatherings(count, lastDay);
        } catch (IllegalArgumentException exception) {
            log.warn("Clearing unreadable settlement gatherings: {}", exception.getMessage());
            return SettlementGatherings.NONE;
        }
    }

    private static SettlementRoster roster(List<SettlementRosterEntry> entries) {
        // A roster rejects a member listed twice, which would cost the whole settlement; dropping the repeat costs one row
        Map<UUID, SettlementRosterEntry> entriesByMember = new LinkedHashMap<>();
        for (SettlementRosterEntry entry : entries) {
            if (entriesByMember.putIfAbsent(entry.memberId(), entry) != null) {
                log.warn("Dropping a repeated settlement roster entry for member {}", entry.memberId());
            }
        }
        return new SettlementRoster(List.copyOf(entriesByMember.values()));
    }

    private static DataResult<SettlementFading> fading(long elapsedTicks) {
        try {
            return DataResult.success(new SettlementFading(ClockTicks.of(elapsedTicks)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(exception::getMessage);
        }
    }

    private static StoredState readState(int formatVersion, List<Settlement> settlements, List<UUID> tombstones) {
        if (formatVersion > FORMAT_VERSION) {
            log.warn("Settlement store has format version {}, newer than the {} this build reads; data this build cannot "
                    + "read is lost when the store is next saved", formatVersion, FORMAT_VERSION);
        }
        return new StoredState(settlements, tombstones);
    }

    /**
     * A dimension's store as saved, in stored order.
     *
     * @param settlements the stored settlements
     * @param tombstones  the ids of the generated settlements disbanded from the dimension
     */
    public record StoredState(List<Settlement> settlements, List<UUID> tombstones) {

        public StoredState {
            settlements = List.copyOf(settlements);
            tombstones = List.copyOf(tombstones);
        }

    }

}
