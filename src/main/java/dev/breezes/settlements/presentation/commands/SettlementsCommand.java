package dev.breezes.settlements.presentation.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome.Founded;
import dev.breezes.settlements.application.settlement.founding.SettlementFoundingOutcome.Refused;
import dev.breezes.settlements.application.settlement.health.SettlementHealthService;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.query.BuildingContext;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import dev.breezes.settlements.domain.generation.history.HistoryEvent;
import dev.breezes.settlements.domain.generation.model.profile.SettlementProfile;
import dev.breezes.settlements.domain.generation.model.profile.TraitId;
import dev.breezes.settlements.domain.settlement.health.SettlementHealth;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementBounds;
import dev.breezes.settlements.domain.settlement.model.SettlementFading;
import dev.breezes.settlements.domain.settlement.model.SettlementFounding;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementOrigin;
import dev.breezes.settlements.domain.settlement.model.SettlementRosterEntry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.domain.world.ChunkPosition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * The /settlements command, through which operators list a dimension's settlements, inspect one by its id or by a
 * position it claims, found one at a bell, and disband one by its id. Its output is English and never translated,
 * since it is an operator's debug tool.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SettlementsCommand {

    private static final String ARG_DIMENSION = "dimension";
    private static final String ARG_ID = "id";
    private static final String ARG_POSITION = "position";

    /**
     * How many of a settlement's latest log entries inspecting it shows.
     */
    private static final int RECENT_LOG_ENTRIES = 5;

    private static final Dynamic2CommandExceptionType ERROR_NONE_HERE = new Dynamic2CommandExceptionType(
            (position, dimension) -> Component.literal("No settlement claims the chunk at " + position + " in "
                    + dimension));
    private static final Dynamic2CommandExceptionType ERROR_UNKNOWN_ID = new Dynamic2CommandExceptionType(
            (id, dimension) -> Component.literal("There is no settlement with id " + id + " in " + dimension));
    private static final SimpleCommandExceptionType ERROR_NO_LOOK_TARGET = new SimpleCommandExceptionType(
            Component.literal("Look at a bell within reach, or give the bell's position"));
    private static final DynamicCommandExceptionType ERROR_FOUNDING_REFUSED = new DynamicCommandExceptionType(
            reason -> (Component) reason);

    public static void register(@Nonnull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("settlements")
                .requires(source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("list")
                        .executes(context -> list(context.getSource(), context.getSource().getLevel()))
                        .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                .executes(context -> list(context.getSource(),
                                        DimensionArgument.getDimension(context, ARG_DIMENSION)))))
                .then(Commands.literal("here")
                        .executes(context -> here(context.getSource())))
                .then(Commands.literal("found")
                        .executes(context -> foundAtLookTarget(context.getSource()))
                        .then(Commands.argument(ARG_POSITION, BlockPosArgument.blockPos())
                                .executes(context -> found(context.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(context, ARG_POSITION)))))
                .then(Commands.literal("inspect")
                        .then(Commands.argument(ARG_ID, UuidArgument.uuid())
                                .suggests(SettlementsCommand::suggestIds)
                                .executes(context -> inspect(context.getSource(),
                                        UuidArgument.getUuid(context, ARG_ID), context.getSource().getLevel()))
                                .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                        .executes(context -> inspect(context.getSource(),
                                                UuidArgument.getUuid(context, ARG_ID),
                                                DimensionArgument.getDimension(context, ARG_DIMENSION))))))
                .then(Commands.literal("disband")
                        .then(Commands.argument(ARG_ID, UuidArgument.uuid())
                                .suggests(SettlementsCommand::suggestIds)
                                .executes(context -> disband(context.getSource(),
                                        UuidArgument.getUuid(context, ARG_ID), context.getSource().getLevel()))
                                .then(Commands.argument(ARG_DIMENSION, DimensionArgument.dimension())
                                        .executes(context -> disband(context.getSource(),
                                                UuidArgument.getUuid(context, ARG_ID),
                                                DimensionArgument.getDimension(context, ARG_DIMENSION)))))));
    }

    private static CompletableFuture<Suggestions> suggestIds(@Nonnull CommandContext<CommandSourceStack> context,
                                                             @Nonnull SuggestionsBuilder builder) {
        // The dimension argument follows the id, so the ids suggested are those of the source's own dimension, which
        // is also the one used when no dimension is given
        return SharedSuggestionProvider.suggest(store().all(context.getSource().getLevel()).stream()
                .map(settlement -> settlement.id().toString()), builder);
    }

    private static int list(@Nonnull CommandSourceStack source, @Nonnull ServerLevel level) {
        List<Settlement> settlements = store().all(level);
        String dimension = dimensionName(level);
        if (settlements.isEmpty()) {
            source.sendSuccess(() -> Component.literal("There are no settlements in " + dimension), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("There are " + settlements.size() + " settlement(s) in "
                + dimension + ":"), false);
        for (Settlement settlement : settlements) {
            SettlementBounds bounds = settlement.bounds();
            source.sendSuccess(() -> Component.empty()
                    .append(inspectLink(settlement, dimension))
                    .append(" (" + settlement.id() + "): " + bounds.chunks().size() + " chunks around chunk "
                            + chunk(bounds.center())), false);
        }
        return settlements.size();
    }

    private static int here(@Nonnull CommandSourceStack source) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        BlockPos position = BlockPos.containing(source.getPosition());
        SettlementQueryService queryService = SettlementsDagger.serverOrThrow().settlementQueryService();
        Optional<Settlement> settlement = queryService.getSettlementAt(level, position);
        if (settlement.isEmpty()) {
            throw ERROR_NONE_HERE.create(position.toShortString(), dimensionName(level));
        }

        describe(source, settlement.get(), level);
        // Only a generated settlement's buildings are known, so for any other the line would always say none
        if (settlement.get().origin() == SettlementOrigin.GENERATED) {
            Optional<BuildingContext> building = queryService.getBuildingAt(level, position);
            source.sendSuccess(() -> Component.literal("Building here: "
                    + building.map(BuildingContext::displayName).orElse("none")), false);
        }
        return 1;
    }

    private static int inspect(@Nonnull CommandSourceStack source, @Nonnull UUID settlementId,
                               @Nonnull ServerLevel level) throws CommandSyntaxException {
        Optional<Settlement> settlement = store().find(level, settlementId);
        if (settlement.isEmpty()) {
            throw ERROR_UNKNOWN_ID.create(settlementId.toString(), dimensionName(level));
        }

        describe(source, settlement.get(), level);
        return 1;
    }

    private static int disband(@Nonnull CommandSourceStack source, @Nonnull UUID settlementId,
                               @Nonnull ServerLevel level) throws CommandSyntaxException {
        Optional<Settlement> disbanded = SettlementsDagger.serverOrThrow()
                .settlementHealthService()
                .disband(level, settlementId);
        if (disbanded.isEmpty()) {
            throw ERROR_UNKNOWN_ID.create(settlementId.toString(), dimensionName(level));
        }

        Settlement settlement = disbanded.get();
        boolean tombstoned = store().isTombstoned(level, settlementId);
        source.sendSuccess(() -> Component.literal("Disbanded " + settlement.name() + " ")
                .append(ComponentUtils.copyOnClickText(settlement.id().toString()))
                .append(" in " + dimensionName(level) + (tombstoned
                        ? ", leaving a tombstone so its structure start does not record it again"
                        : ", leaving no tombstone")), true);
        return 1;
    }

    private static int foundAtLookTarget(@Nonnull CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult target = player.pick(player.blockInteractionRange(), 1.0F, false);
        // A miss is a BlockHitResult too, still answering the position where the ray stopped, so only the type tells
        if (target.getType() != HitResult.Type.BLOCK) {
            throw ERROR_NO_LOOK_TARGET.create();
        }
        return found(source, ((BlockHitResult) target).getBlockPos());
    }

    private static int found(@Nonnull CommandSourceStack source, @Nonnull BlockPos bell) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        // No residents are required, so an operator can found at an empty bell; every other rule applies
        CompletableFuture<SettlementFoundingOutcome> founding = SettlementsDagger.serverOrThrow()
                .settlementFoundingService()
                .found(level, bell, 0);
        if (founding.getNow(null) instanceof Refused refused) {
            throw ERROR_FOUNDING_REFUSED.create(refusal(refused, bell, level));
        }

        source.sendSuccess(() -> Component.literal("Surveying the land around the bell at " + bell.toShortString() + "..."), false);
        founding.whenComplete((outcome, failure) -> {
            if (failure != null) {
                source.sendFailure(Component.literal("Founding a settlement at the bell at " + bell.toShortString()
                        + " failed; the server log has the cause"));
                return;
            }
            switch (outcome) {
                case Founded founded -> reportFounded(source, founded, level);
                case Refused refused -> source.sendFailure(refusal(refused, bell, level));
            }
        });
        return 1;
    }

    private static void reportFounded(@Nonnull CommandSourceStack source, @Nonnull Founded founded,
                                      @Nonnull ServerLevel level) {
        Settlement settlement = founded.settlement();
        int claimed = settlement.bounds().chunks().size();
        int requested = founded.requestedClaim().chunks().size();
        source.sendSuccess(() -> Component.literal("Founded " + settlement.name() + " ")
                .append(ComponentUtils.copyOnClickText(settlement.id().toString()))
                .append(", " + (settlement.founding().isEstablished() ? "established" : "new") + ", claiming "
                        + claimed + " of the " + requested + " chunks requested, with " + founded.members().size()
                        + " founding member(s)"), true);
        describe(source, settlement, level);
    }

    private static Component refusal(@Nonnull Refused refused, @Nonnull BlockPos bell, @Nonnull ServerLevel level) {
        String theBell = "the bell at " + bell.toShortString();
        return switch (refused.reason()) {
            case DIMENSION_EXCLUDED -> Component.literal("Settlements cannot be founded in " + dimensionName(level));
            case BELL_UNLOADED -> Component.literal("The position " + bell.toShortString() + " is not loaded");
            case NO_BELL -> Component.literal("There is no bell at " + bell.toShortString());
            case IN_FLIGHT -> Component.literal("A settlement is already being founded at " + theBell);
            case CLAIMED -> Component.literal("The bell at " + bell.toShortString() + " stands in ")
                    .append(inspectLink(Objects.requireNonNull(refused.claimant()), dimensionName(level)));
            case TOO_FEW_FOUNDERS -> Component.literal("Only " + refused.found() + " unassigned villager(s) gather at "
                    + theBell + ", and founding needs " + refused.required());
            case TOO_FEW_CHUNKS -> Component.literal("Only " + refused.found() + " chunk(s) around " + theBell
                    + " are free of other settlements, and founding needs " + refused.required());
            case UNNAMED -> Component.literal("The bell has no name, and none can be generated: the settlement "
                    + "name pools are empty");
            case NOT_STORED -> Component.literal("The settlement founded at " + theBell + " could not be stored");
        };
    }

    private static void describe(@Nonnull CommandSourceStack source, @Nonnull Settlement settlement,
                                 @Nonnull ServerLevel level) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(settlement.name() + " ")
                .append(ComponentUtils.copyOnClickText(settlement.id().toString()))
                .append(": " + origin(settlement.origin()) + ", in " + dimensionName(level)));

        SettlementBounds bounds = settlement.bounds();
        lines.add(Component.literal("Claimed chunks: " + bounds.chunks().size() + ", around chunk "
                + chunk(bounds.center()) + " ").append(teleportLink(bounds.center(), level)));

        SettlementFounding founding = settlement.founding();
        lines.add(Component.literal("Founded on day " + founding.founded().calendarDay() + ", recorded from day "
                + founding.recorded().calendarDay() + ": " + (founding.isEstablished() ? "established" : "new")));

        SettlementProfile profile = settlement.profile();
        lines.add(Component.literal("Profile: " + profile.scaleTier().name() + ", initial population "
                + profile.initialPopulation() + ", wealth " + String.format(Locale.ROOT, "%.2f",
                profile.wealthLevel()) + ", defense " + profile.defenseLevel().name()));
        lines.add(Component.literal("Traits: primary " + profile.primary().full() + ", secondary "
                + traits(profile.secondary()) + ", flavor " + traits(profile.flavor())));

        List<HistoryEvent> history = settlement.history();
        if (history.isEmpty()) {
            lines.add(Component.literal("History: none"));
        } else {
            lines.add(Component.literal("History (" + history.size() + " events):"));
            for (HistoryEvent event : history) {
                lines.add(Component.literal("  Day " + event.date().calendarDay() + ": " + event.eventId()));
            }
        }

        SettlementGatherings gatherings = settlement.gatherings();
        lines.add(Component.literal(gatherings.count() == 0
                ? "Gatherings: none"
                : "Gatherings: " + gatherings.count() + ", the last on day " + gatherings.lastDay()));
        SettlementHealthService healthService = SettlementsDagger.serverOrThrow().settlementHealthService();
        lines.add(Component.literal("Health: " + health(healthService.assess(level, settlement))));
        lines.add(Component.literal("Fading: " + fading(settlement.fading(),
                healthService.fadingDuration().orElse(null))));

        List<SettlementLogEntry> log = settlement.log().entries();
        if (log.isEmpty()) {
            lines.add(Component.literal("Log: empty"));
        } else {
            // The list order is the order events happened in; their dates can run backward after a time rewind
            List<SettlementLogEntry> recent = log.subList(Math.max(0, log.size() - RECENT_LOG_ENTRIES), log.size());
            lines.add(Component.literal("Latest log entries (" + recent.size() + " of " + log.size() + "):"));
            for (SettlementLogEntry entry : recent) {
                lines.add(Component.literal("  Day " + entry.date().calendarDay() + ": " + logEntry(entry)));
            }
        }

        // Last, one member to a line, because a town's roster runs to dozens of rows and would bury the sections above
        List<SettlementRosterEntry> roster = settlement.roster().entries();
        if (roster.isEmpty()) {
            lines.add(Component.literal("Roster: empty"));
        } else {
            VillagerNameDirectory names = SettlementsDagger.serverOrThrow().villagerNameDirectory();
            lines.add(Component.literal("Roster (" + roster.size() + " members):"));
            for (SettlementRosterEntry entry : roster) {
                lines.add(Component.literal("  ").append(memberName(entry.memberId(), names)).append(": "
                        + rosterEntry(entry, gatherings)));
            }
        }

        lines.forEach(line -> source.sendSuccess(() -> line, false));
    }

    private static String health(@Nonnull SettlementHealth health) {
        String status = switch (health.status()) {
            case HEALTHY -> "healthy";
            case UNHEALTHY -> "unhealthy (" + String.join(", ", health.causes().stream()
                    .map(SettlementsCommand::cause)
                    .toList()) + ")";
            case UNDETERMINED -> "undetermined";
        };
        String bell = switch (health.bell()) {
            case FOUND -> "a bell found";
            case MISSING -> "no bell in the claim";
            case NOT_KNOWABLE -> "no bell in its loaded chunks, and some claimed chunks are not loaded";
        };
        return status + "; " + health.livingMembers() + " living member(s) of the " + health.minimumResidents()
                + " needed; " + bell;
    }

    private static String cause(@Nonnull SettlementHealth.Cause cause) {
        return switch (cause) {
            case NO_BELL -> "no bell";
            case TOO_FEW_RESIDENTS -> "too few residents";
        };
    }

    /**
     * @param duration the fading duration, or null when decay is disabled
     */
    private static String fading(@Nullable SettlementFading fading, @Nullable ClockTicks duration) {
        if (fading == null) {
            return duration == null ? "no, and decay is disabled" : "no";
        }
        long spent = fading.elapsed().getTicks();
        if (duration == null) {
            return "for " + days(spent) + " loaded days (" + spent + " ticks), held while decay is disabled";
        }
        return "for " + days(spent) + " of " + days(duration.getTicks()) + " loaded days (" + spent + " of "
                + duration.getTicks() + " ticks)";
    }

    private static String days(long ticks) {
        return String.format(Locale.ROOT, "%.1f", (double) ticks / TimeOfDay.TICKS_PER_DAY);
    }

    private static String rosterEntry(@Nonnull SettlementRosterEntry entry, @Nonnull SettlementGatherings gatherings) {
        String row = entry.profession().id() + ", " + (entry.adult() ? "adult" : "baby") + ", "
                + (entry.housed() ? "housed" : "unhoused");
        // Babies never check in, so the gatherings held since a baby's mark do not mean it was away
        if (!entry.adult()) {
            return row;
        }
        return row + ", missed " + (gatherings.count() - entry.gatheringCountAtLastCheckIn()) + " gathering(s)";
    }

    private static Component memberName(@Nonnull UUID memberId, @Nonnull VillagerNameDirectory names) {
        return Component.literal(names.resolve(memberId)).withStyle(style -> style
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, memberId.toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.literal(memberId + "\nClick to copy the UUID"))));
    }

    private static String logEntry(@Nonnull SettlementLogEntry entry) {
        List<String> parts = new ArrayList<>();
        parts.add(entry.type().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        if (entry.subjectName() != null) {
            parts.add(entry.subjectName());
        } else if (entry.subjectId() != null) {
            parts.add(entry.subjectId().toString());
        }
        if (entry.detail() != null) {
            parts.add(entry.detail());
        }
        return String.join(", ", parts);
    }

    private static String origin(@Nonnull SettlementOrigin origin) {
        return switch (origin) {
            case GENERATED -> "generated with its structure";
            case FOUNDED -> "founded at a bell";
        };
    }

    private static String traits(@Nonnull List<TraitId> traits) {
        return traits.isEmpty() ? "none" : String.join(", ", traits.stream().map(TraitId::full).toList());
    }

    private static String chunk(@Nonnull ChunkPosition chunk) {
        return "(" + chunk.x() + ", " + chunk.z() + ")";
    }

    private static Component inspectLink(@Nonnull Settlement settlement, @Nonnull String dimension) {
        String command = "/settlements inspect " + settlement.id() + " " + dimension;
        return ComponentUtils.wrapInSquareBrackets(Component.literal(settlement.name())).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to inspect"))));
    }

    private static Component teleportLink(@Nonnull ChunkPosition chunk, @Nonnull ServerLevel level) {
        // Runs in the settlement's dimension, so the link also works from another one
        ChunkPos center = new ChunkPos(chunk.x(), chunk.z());
        String command = "/execute in " + dimensionName(level) + " run tp @s " + center.getMiddleBlockX() + " ~ "
                + center.getMiddleBlockZ();
        return ComponentUtils.wrapInSquareBrackets(Component.literal(center.getMiddleBlockX() + ", ~, "
                + center.getMiddleBlockZ())).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Click to teleport"))));
    }

    private static String dimensionName(@Nonnull ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static SettlementStore store() {
        return SettlementsDagger.serverOrThrow().settlementStore();
    }

}
