package dev.breezes.settlements.application.settlement.founding;

import dev.breezes.settlements.application.settlement.bell.SettlementBellRules;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome.Founded;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome.Reason;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome.Refused;
import dev.breezes.settlements.application.settlement.lifecycle.SettlementLifecyclePublisher;
import dev.breezes.settlements.application.settlement.membership.SettlementMembershipService;
import dev.breezes.settlements.application.settlement.naming.SettlementNamer;
import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder;
import dev.breezes.settlements.application.settlement.persistence.GeneratedSettlementRecorder.GeneratedSettlement;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.di.SettlementFoundingExecutor;
import dev.breezes.settlements.domain.generation.history.HistoryEventEngine;
import dev.breezes.settlements.domain.generation.history.SettlementFoundingKind;
import dev.breezes.settlements.domain.generation.model.SettlementCharacter;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.generation.pipeline.GenerationPipeline;
import dev.breezes.settlements.domain.settlement.founding.SettlementFoundingClaim;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.tags.SettlementsDimensionTags;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.tags.SettlementsStructureTags;
import dev.breezes.settlements.domain.world.ChunkPosition;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.TerrainGridFactory;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Service owning the founding of settlements at bells.
 * <p>
 * Must be called on the server thread.
 */
@ServerSide
@ServerScope
@CustomLog(topic = "settlement")
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementFoundingService {

    private final SettlementStore settlementStore;
    private final GeneratedSettlementStarts generatedSettlementStarts;
    private final GeneratedSettlementRecorder generatedSettlementRecorder;
    private final SettlementNamer settlementNamer;
    private final SettlementMembershipService membershipService;
    private final SettlementLifecyclePublisher lifecyclePublisher;
    private final GenerationPipeline generationPipeline;
    private final SettlementFoundingConfig config;

    @SettlementFoundingExecutor
    private final ExecutorService foundingExecutor;

    /**
     * The bells with a founding in flight. Read and written on the server thread only.
     */
    private final Set<GlobalPos> bellsInFlight = new HashSet<>();

    /**
     * Attempts to found a settlement at the bell using nearby, unassigned Settlements villagers that have it as their
     * meeting point. On success, the settlement is stored, its founders become members, and the founding is reported to
     * the {@link SettlementLifecyclePublisher}.
     * <p>
     * The returned future may already be complete; otherwise, it completes on the server thread.
     * If the server stops or the level unloads before the attempt finishes, the attempt is abandoned.
     *
     * @param bell             the bell's position in the level
     * @param minimumResidents the minimum number of founding members required for success
     * @return a future reporting {@link Founded} or {@link Refused}; failures during asynchronous founding complete it
     * exceptionally
     * @throws IllegalArgumentException if minimumResidents is negative
     */
    public CompletableFuture<SettlementFoundingOutcome> found(@Nonnull ServerLevel level,
                                                              @Nonnull BlockPos bell,
                                                              int minimumResidents) {
        Validate.isTrue(minimumResidents >= 0, "Minimum residents must not be negative: %d", minimumResidents);
        BlockPos bellPosition = bell.immutable();

        if (!dimensionAllowsFounding(level)) {
            return CompletableFuture.completedFuture(Refused.of(Reason.DIMENSION_EXCLUDED));
        }

        Optional<Refused> bellRefusal = bellRefusal(level, bellPosition);
        if (bellRefusal.isPresent()) {
            return CompletableFuture.completedFuture(bellRefusal.get());
        }

        GlobalPos bellPos = GlobalPos.of(level.dimension(), bellPosition);
        if (this.bellsInFlight.contains(bellPos)) {
            return CompletableFuture.completedFuture(Refused.of(Reason.IN_FLIGHT));
        }

        ChunkPosition bellChunk = chunkOf(bellPosition);
        Optional<Settlement> claimant = this.settlementStore.findClaiming(level, bellChunk);
        if (claimant.isEmpty()) {
            // A generated settlement is recorded only once its start chunk loads, which may not have happened yet.
            // Recording it first makes the bell fall to that settlement instead of founding a second one over it.
            for (GeneratedSettlement generated : this.generatedSettlementStarts.covering(level, bellChunk)) {
                this.generatedSettlementRecorder.record(level, generated);
            }
            claimant = this.settlementStore.findClaiming(level, bellChunk);
        }

        if (claimant.isPresent()) {
            // Already claimed by an existing settlement
            return CompletableFuture.completedFuture(Refused.claimedBy(claimant.get()));
        }

        // Before the founders are counted and the land surveyed, which every attempt at a bell that can never be named
        // would otherwise repeat
        if (bellName(level, bellPosition).isEmpty() && !this.settlementNamer.canGenerateNames()) {
            return CompletableFuture.completedFuture(Refused.of(Reason.UNNAMED));
        }

        List<BaseVillager> founders = founders(level, bellPos);
        if (founders.size() < minimumResidents) {
            return CompletableFuture.completedFuture(Refused.shortOf(Reason.TOO_FEW_FOUNDERS, founders.size(), minimumResidents));
        }

        SettlementFoundingKind kind = standsInVillage(level, bellPosition)
                ? SettlementFoundingKind.ESTABLISHED
                : SettlementFoundingKind.NEW;

        SettlementBounds requestedClaim = SettlementFoundingClaim.square(bellChunk, founderPointsOfInterest(level, founders));
        SettlementBounds claim = this.clipToFreeChunks(level, requestedClaim);
        int minimumChunks = this.config.minimumFoundingChunks();
        if (!SettlementFoundingClaim.isLargeEnough(claim, minimumChunks)) {
            return CompletableFuture.completedFuture(Refused.shortOf(Reason.TOO_FEW_CHUNKS, claim.chunks().size(), minimumChunks));
        }

        int headcount = headcount(level, claim);
        Founding founding = new Founding(UUID.randomUUID(), bellPos, bellChunk, requestedClaim, founders,
                minimumResidents, kind, ScaleTier.forHeadcount(headcount), headcount,
                new CivilDateTime(level.getDayTime()));

        // The worker reads only the generator's noise through these, never the level, whose chunks and entities
        // belong to the server thread
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        LevelHeightAccessor heights = LevelHeightAccessor.create(level.getMinBuildHeight(), level.getHeight());
        MinecraftServer server = level.getServer();

        CompletableFuture<SettlementFoundingOutcome> outcome = new CompletableFuture<>();
        this.foundingExecutor.execute(() -> {
            Surveyed surveyed;
            try {
                surveyed = this.survey(founding, generator, randomState, heights);
            } catch (RuntimeException exception) {
                server.execute(() -> this.finish(level, founding, outcome, () -> {
                    throw exception;
                }));
                return;
            }
            server.execute(() -> this.finish(level, founding, outcome, () -> this.commit(level, founding, surveyed)));
        });
        // Held only once the worker accepted the founding. Its finish cannot run before this: it is posted to the
        // server thread, which is busy here until this method returns.
        this.bellsInFlight.add(bellPos);
        return outcome;
    }

    private Surveyed survey(Founding founding,
                            ChunkGenerator generator,
                            RandomState randomState,
                            LevelHeightAccessor heights) {
        BlockPos bell = founding.bell().pos();
        SurveyBounds bounds = SurveyBounds.aroundSettlementAt(bell.getX(), bell.getZ());
        TerrainGrid terrain = TerrainGridFactory.fromNoise(generator, randomState, heights, bounds);
        UUID id = founding.id();
        SettlementCharacter character = this.generationPipeline.character(terrain, bounds, founding.tier(),
                founding.headcount(), founding.kind(), id.getMostSignificantBits() ^ id.getLeastSignificantBits());
        // Seeded from the profile, as a generated settlement's dates are
        HistoryEventEngine.DatedHistory history = HistoryEventEngine.date(character.history().events(),
                founding.recorded(), founding.kind(), new Random(character.profile().seed()));
        return new Surveyed(character.profile(), history);
    }

    private void finish(ServerLevel level,
                        Founding founding,
                        CompletableFuture<SettlementFoundingOutcome> outcome,
                        Supplier<SettlementFoundingOutcome> result) {
        MinecraftServer server = level.getServer();
        // Once the server has stopped, execute runs this inline on the worker that posted it, and the store may
        // already be saved. Touching nothing then is what drops the founding.
        if (!server.isSameThread() || !server.isRunning()) {
            return;
        }
        this.bellsInFlight.remove(founding.bell());
        if (server.getLevel(level.dimension()) != level) {
            return;
        }

        try {
            outcome.complete(result.get());
        } catch (RuntimeException exception) {
            log.error("Founding a settlement at {} failed", founding.bell(), exception);
            outcome.completeExceptionally(exception);
        }
    }

    private SettlementFoundingOutcome commit(ServerLevel level, Founding founding, Surveyed surveyed) {
        // Re-check the bell and claims since the surveying was computed asynchronously
        BlockPos bell = founding.bell().pos();
        Optional<Refused> bellRefusal = bellRefusal(level, bell);
        if (bellRefusal.isPresent()) {
            return bellRefusal.get();
        }

        Optional<Settlement> claimant = this.settlementStore.findClaiming(level, founding.bellChunk());
        if (claimant.isPresent()) {
            return Refused.claimedBy(claimant.get());
        }

        // Another settlement may have claimed part of the square meanwhile, as a generated start chunk loading does
        SettlementBounds claim = this.clipToFreeChunks(level, founding.requestedClaim());
        int minimumChunks = this.config.minimumFoundingChunks();
        if (!SettlementFoundingClaim.isLargeEnough(claim, minimumChunks)) {
            return Refused.shortOf(Reason.TOO_FEW_CHUNKS, claim.chunks().size(), minimumChunks);
        }

        // Checked again: a reload can empty the name pools during the survey
        Optional<String> name = this.settlementName(level, bell, surveyed.profile().seed());
        if (name.isEmpty()) {
            return Refused.of(Reason.UNNAMED);
        }

        // During the survey a founder may have died, left the level, or joined a settlement
        List<BaseVillager> members = founding.founders().stream()
                .filter(founder -> founder.isAlive()
                        && founder.level() == level
                        && !founder.hasData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP))
                .toList();
        if (members.size() < founding.minimumResidents()) {
            return Refused.shortOf(Reason.TOO_FEW_FOUNDERS, members.size(), founding.minimumResidents());
        }

        CivilDateTime recorded = founding.recorded();
        Settlement settlement = Settlement.builder()
                .id(founding.id())
                .name(name.get())
                .origin(SettlementOrigin.FOUNDED)
                .bounds(claim)
                .founding(new SettlementFounding(surveyed.history().founded(), recorded))
                .profile(surveyed.profile())
                .history(surveyed.history().events())
                .roster(SettlementRoster.EMPTY)
                .gatherings(SettlementGatherings.NONE)
                .log(new SettlementLog(List.of(SettlementLogEntry.builder()
                        .date(recorded)
                        .type(SettlementLogType.FOUNDED)
                        .build())))
                .build();
        Optional<Settlement> stored = this.settlementStore.add(level, settlement);
        if (stored.isEmpty()) {
            return Refused.of(Reason.NOT_STORED);
        }

        // Stored above on this thread, so the join finds it
        Settlement founded = this.membershipService.joinFounders(level, stored.get().id(), members).orElseThrow();

        log.info("Founded settlement {} ({}) at {} in {}, claiming {} chunks, with {} founding members",
                founded.name(), founded.id(), bell, level.dimension().location(),
                founded.bounds().chunks().size(), members.size());
        this.lifecyclePublisher.founded(level, founded);
        return new Founded(founded, founding.requestedClaim(), members);
    }

    private static List<BaseVillager> founders(ServerLevel level, GlobalPos bell) {
        Vec3 center = Vec3.atCenterOf(bell.pos());
        double rangeSqr = (double) AcquirePoi.SCAN_RANGE * AcquirePoi.SCAN_RANGE;
        return level.getEntitiesOfClass(BaseVillager.class, new AABB(bell.pos()).inflate(AcquirePoi.SCAN_RANGE),
                villager -> villager.isAlive()
                        // A founding never takes another settlement's members
                        && !villager.hasData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)
                        && villager.distanceToSqr(center) <= rangeSqr
                        && villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT).filter(bell::equals).isPresent());
    }

    private static List<ChunkPosition> founderPointsOfInterest(ServerLevel level, List<BaseVillager> founders) {
        return founders.stream()
                .flatMap(founder -> Stream.of(MemoryModuleType.HOME, MemoryModuleType.JOB_SITE)
                        .flatMap(memory -> founder.getBrain().getMemory(memory).stream()))
                // A villager that traveled may keep a bed or workstation in the dimension it left, which says nothing
                // about where it lives in this one
                .filter(place -> place.dimension().equals(level.dimension()))
                .map(place -> chunkOf(place.pos()))
                .toList();
    }

    private static boolean standsInVillage(ServerLevel level, BlockPos bell) {
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Predicate<Structure> isVillage = structure -> structures.wrapAsHolder(structure)
                .is(SettlementsStructureTags.VILLAGES);
        // The start's box, and only in x and z: a bell between houses lies outside every piece, and a village street
        // piece is a block or two tall, so a bell standing on one lies above it
        return level.structureManager().startsForStructure(new ChunkPos(bell), isVillage).stream()
                .map(StructureStart::getBoundingBox)
                .anyMatch(box -> coversColumn(box, bell));
    }

    private static boolean coversColumn(BoundingBox box, BlockPos position) {
        return box.minX() <= position.getX() && position.getX() <= box.maxX()
                && box.minZ() <= position.getZ() && position.getZ() <= box.maxZ();
    }

    private static int headcount(ServerLevel level, SettlementBounds claim) {
        int minChunkX = claim.chunks().stream().mapToInt(ChunkPosition::x).min().orElseThrow();
        int maxChunkX = claim.chunks().stream().mapToInt(ChunkPosition::x).max().orElseThrow();
        int minChunkZ = claim.chunks().stream().mapToInt(ChunkPosition::z).min().orElseThrow();
        int maxChunkZ = claim.chunks().stream().mapToInt(ChunkPosition::z).max().orElseThrow();
        AABB area = new AABB(SectionPos.sectionToBlockCoord(minChunkX), level.getMinBuildHeight(),
                SectionPos.sectionToBlockCoord(minChunkZ), SectionPos.sectionToBlockCoord(maxChunkX + 1),
                level.getMaxBuildHeight(), SectionPos.sectionToBlockCoord(maxChunkZ + 1));
        // The lookup sees only loaded entity sections, so villagers in unloaded chunks go uncounted
        return level.getEntities((Entity) null, area, entity -> entity.isAlive()
                && entity.getType().is(SettlementsEntityTypeTags.SETTLEMENT_RESIDENTS)
                && claim.contains(new ChunkPosition(entity.chunkPosition().x, entity.chunkPosition().z))).size();
    }

    private SettlementBounds clipToFreeChunks(ServerLevel level, SettlementBounds requestedClaim) {
        // The claim's center is the bell's chunk, which the caller has found unclaimed, so the clip always keeps it
        return requestedClaim.clip(chunk -> this.settlementStore.findClaiming(level, chunk).isPresent()).orElseThrow();
    }

    private Optional<String> settlementName(ServerLevel level, BlockPos bell, long seed) {
        return bellName(level, bell)
                .map(preferred -> this.settlementNamer.preferredName(level, preferred))
                .or(() -> this.settlementNamer.generatedName(level, seed));
    }

    private static Optional<String> bellName(ServerLevel level, BlockPos bell) {
        BlockEntity bellEntity = level.getBlockEntity(bell);
        return bellEntity == null ? Optional.empty() : SettlementBellRules.settlementName(bellEntity.components());
    }

    /**
     * Returns whether settlements can be founded in the level's dimension, as its tags are bound now.
     */
    public static boolean dimensionAllowsFounding(@Nonnull ServerLevel level) {
        return level.registryAccess().registryOrThrow(Registries.LEVEL_STEM)
                .getHolder(Registries.levelToLevelStem(level.dimension()))
                .map(stem -> stem.is(SettlementsDimensionTags.ALLOWS_FOUNDING))
                .orElse(false);
    }

    private static Optional<Refused> bellRefusal(ServerLevel level, BlockPos bell) {
        // Loaded first, so the meeting point lookup reads the POI section loaded with the chunk rather than from disk.
        // getChunkNow, not isLoaded: isLoaded turns true once the chunk is due to load, before loading reads its column
        if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(bell.getX()),
                SectionPos.blockToSectionCoord(bell.getZ())) == null) {
            return Optional.of(Refused.of(Reason.BELL_UNLOADED));
        }
        if (!level.getPoiManager().exists(bell, SettlementBellRules.POI_TYPE)) {
            return Optional.of(Refused.of(Reason.NO_BELL));
        }
        return Optional.empty();
    }

    private static ChunkPosition chunkOf(BlockPos position) {
        return new ChunkPosition(SectionPos.blockToSectionCoord(position.getX()),
                SectionPos.blockToSectionCoord(position.getZ()));
    }

    /**
     * What a founding carries from the server thread, where it began, to the worker and back.
     *
     * @param id        the settlement's id; its seed derives from it
     * @param headcount the residents counted in its claim, which become its initial population
     * @param recorded  when the settlement's record begins: the moment the founding began
     */
    private record Founding(
            UUID id,
            GlobalPos bell,
            ChunkPosition bellChunk,
            SettlementBounds requestedClaim,
            List<BaseVillager> founders,
            int minimumResidents,
            SettlementFoundingKind kind,
            ScaleTier tier,
            int headcount,
            CivilDateTime recorded
    ) {
    }

    /**
     * The character computed off the server thread, with its history dated.
     */
    private record Surveyed(SettlementProfile profile, HistoryEventEngine.DatedHistory history) {
    }

}
