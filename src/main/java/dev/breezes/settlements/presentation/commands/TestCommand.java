package dev.breezes.settlements.presentation.commands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.breezes.settlements.application.ai.dialogue.Occasion;
import dev.breezes.settlements.application.ai.inference.InferenceCapability;
import dev.breezes.settlements.application.ai.inference.InferenceTransport;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueBatchRequest;
import dev.breezes.settlements.application.ai.inference.monologue.MonologueRequestAssembler;
import dev.breezes.settlements.di.ServerComponent;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.ai.memory.MemoryType;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.generation.building.BuildingRegistry;
import dev.breezes.settlements.domain.settlement.model.SettlementMetadata;
import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import dev.breezes.settlements.domain.settlement.query.SettlementPositionContext;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.query.SettlementStructureLocator;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementBuildingPiece;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.pieces.SettlementRoadPiece;
import dev.breezes.settlements.infrastructure.network.features.debug.packet.ClientBoundSettlementDebugPacket;
import dev.breezes.settlements.shared.util.VillagerRaycastUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class TestCommand {

    private static final String ARG_OCCASION = "occasion";
    private static final String OCCASION_ARG_ALL = "all";
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SETTLEMENT_DEBUG_MIN_Y_OFFSET = -5;
    private static final int SETTLEMENT_DEBUG_MAX_Y_OFFSET = 30;
    private static final DateTimeFormatter GENERATION_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("info").executes(TestCommand::settlementInfo))
                .then(Commands.literal("memory").executes(TestCommand::dumpMemories))
                .then(buildMonologueCommand()));
    }

    private static int settlementInfo(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        SettlementQueryService settlementQueryService = SettlementsDagger.serverOrThrow().settlementQueryService();
        SettlementPositionContext positionContext = settlementQueryService.getContextAt(player.serverLevel(), player.blockPosition());

        if (!positionContext.hasSettlement()) {
            context.getSource().sendSuccess(() -> Component.literal("You are not standing in a settlement."), false);
            return Command.SINGLE_SUCCESS;
        }

        SettlementMetadata settlementMetadata = positionContext.settlement().orElseThrow().metadata();
        context.getSource().sendSuccess(() -> Component.literal("Settlement: " + settlementMetadata.name()), false);
        context.getSource().sendSuccess(() -> Component.literal("Scale tier: " + settlementMetadata.scaleTier()), false);
        context.getSource().sendSuccess(() -> Component.literal("Primary trait: " + settlementMetadata.primaryTrait()), false);
        context.getSource().sendSuccess(() -> Component.literal("Population: " + settlementMetadata.estimatedPopulation()), false);
        context.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Wealth level: %.2f", settlementMetadata.wealthLevel())), false);
        context.getSource().sendSuccess(() -> Component.literal("Center: (" + settlementMetadata.centerX() + ", " + settlementMetadata.centerZ() + ")"), false);

        positionContext.building()
                .map(BuildingContext::displayName)
                .ifPresentOrElse(
                        buildingName -> context.getSource().sendSuccess(() -> Component.literal("Building: " + buildingName), false),
                        () -> context.getSource().sendSuccess(() -> Component.literal("Building: none"), false)
                );

        sendSettlementDebugOverlay(player, settlementMetadata);
        return Command.SINGLE_SUCCESS;
    }

    private static void sendSettlementDebugOverlay(@Nonnull ServerPlayer player,
                                                   @Nonnull SettlementMetadata settlementMetadata) {
        SettlementStructureLocator settlementStructureLocator = SettlementsDagger.serverOrThrow().settlementStructureLocator();
        Optional<StructureStart> structureStart = settlementStructureLocator.locate(player.serverLevel(), player.blockPosition());
        if (structureStart.isEmpty()) {
            return;
        }

        BuildingRegistry buildingRegistry = SettlementsDagger.component().buildingDefinitionDataManager();
        List<StructurePiece> pieces = structureStart.get().getPieces();
        List<ClientBoundSettlementDebugPacket.BuildingBox> buildings = pieces.stream()
                .filter(SettlementBuildingPiece.class::isInstance)
                .map(SettlementBuildingPiece.class::cast)
                .map(piece -> new ClientBoundSettlementDebugPacket.BuildingBox(
                        piece.getBoundingBox(),
                        buildingRegistry.displayNameFor(piece.getBuildingDefinitionId())))
                .toList();
        List<ClientBoundSettlementDebugPacket.RoadBox> roads = pieces.stream()
                .filter(SettlementRoadPiece.class::isInstance)
                .map(SettlementRoadPiece.class::cast)
                .map(piece -> new ClientBoundSettlementDebugPacket.RoadBox(
                        piece.getBoundingBox(),
                        piece.getRoadType(),
                        piece.getStart(),
                        piece.getEnd()))
                .toList();

        int playerY = player.blockPosition().getY();
        BoundingBox settlementBox = new BoundingBox(
                settlementMetadata.boundsMinX(),
                playerY + SETTLEMENT_DEBUG_MIN_Y_OFFSET,
                settlementMetadata.boundsMinZ(),
                settlementMetadata.boundsMaxX(),
                playerY + SETTLEMENT_DEBUG_MAX_Y_OFFSET,
                settlementMetadata.boundsMaxZ());

        PacketDistributor.sendToPlayer(player, new ClientBoundSettlementDebugPacket(settlementBox, buildings, roads));
    }

    private static Optional<ISettlementsVillager> getLookedAtVillager(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return Optional.empty();
        }

        Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(player, 50);
        if (hitResult.isEmpty() || !(hitResult.get().getEntity() instanceof ISettlementsVillager villager)) {
            player.displayClientMessage(Component.literal("No villager found in range"), true);
            return Optional.empty();
        }

        return Optional.of(villager);
    }

    private static int dumpMemories(CommandContext<CommandSourceStack> context) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }
        if (!(villagerOptional.get() instanceof BaseVillager baseVillager)) {
            context.getSource().sendFailure(Component.literal("Targeted entity is not a BaseVillager."));
            return 0;
        }

        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal("[memory] decaying spatial sites for " + baseVillager.getUUID() + ":"), false);

        for (MemoryType.DecayingSpatialMemoryType type : MemoryTypeRegistry.decayingSpatialTypes()) {
            List<GlobalPos> sites = baseVillager.getSettlementsBrain().getMemory(type).orElse(List.of());
            String line = "  " + type.identifier() + " = " + sites.size() + describeSites(sites);
            source.sendSuccess(() -> Component.literal(line), false);
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * Renders up to the first few sites as "(x,y,z)" tuples with a "+N more" suffix when the list
     * is longer. Sites are unordered — the decaying store returns them in store order, not by proximity.
     */
    private static String describeSites(@Nonnull List<GlobalPos> sites) {
        if (sites.isEmpty()) {
            return "";
        }

        int previewCount = Math.min(sites.size(), 5);
        String preview = sites.stream()
                .limit(previewCount)
                .map(globalPos -> {
                    BlockPos pos = globalPos.pos();
                    return "(" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ")";
                })
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        if (sites.size() > previewCount) {
            return " " + preview + " (+" + (sites.size() - previewCount) + " more)";
        }
        return " " + preview;
    }


    private static LiteralArgumentBuilder<CommandSourceStack> buildMonologueCommand() {
        return Commands.literal("monologue")
                .then(Commands.literal("dump")
                        // No occasion arg → use the villager's current occasion
                        .executes(TestCommand::dumpMonologue)
                        // Optional occasion arg: a specific Occasion name or "all"
                        .then(Commands.argument(ARG_OCCASION, StringArgumentType.word())
                                .suggests(TestCommand::suggestOccasions)
                                .executes(TestCommand::dumpMonologue)));
    }

    private static CompletableFuture<Suggestions> suggestOccasions(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        builder.suggest(OCCASION_ARG_ALL);
        for (Occasion occasion : Occasion.values()) {
            builder.suggest(occasion.name().toLowerCase(Locale.ROOT));
        }
        return builder.buildFuture();
    }

    private static int dumpMonologue(CommandContext<CommandSourceStack> context) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }

        ISettlementsVillager villager = villagerOptional.get();
        if (!(villager instanceof BaseVillager baseVillager)) {
            context.getSource().sendFailure(Component.literal("Targeted entity is not a BaseVillager."));
            return 0;
        }

        Optional<List<Occasion>> requestedOccasions = resolveRequestedOccasions(context, baseVillager);
        if (requestedOccasions.isEmpty()) {
            // resolveRequestedOccasions already sent the failure message
            return 0;
        }
        List<Occasion> occasions = requestedOccasions.get();

        ServerComponent server = SettlementsDagger.serverOrThrow();
        MonologueRequestAssembler assembler = server.monologueRequestAssembler();
        InferenceTransport transport = server.inferenceTransport();

        MonologueBatchRequest batchRequest = assembler.assembleForVillager(baseVillager, occasions);

        // Use the sweep deadline as a representative budget — it is what a real overnight sweep
        // would send, so the dump fixture is byte-faithful to production behaviour.
        Duration deadline = Duration.ofSeconds(server.dialogueConfig().packSweepDeadlineSeconds());
        String compactEnvelope = transport.renderEnvelope(InferenceCapability.MONOLOGUE, batchRequest, deadline);

        // Wire body stays compact; only the on-disk fixture is pretty-printed for human inspection.
        String prettyJson = prettyPrint(compactEnvelope);

        Path outputPath = buildMonologueOutputPath();
        try {
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, prettyJson, StandardCharsets.UTF_8);
        } catch (IOException e) {
            context.getSource().sendFailure(Component.literal("Failed to write monologue dump: " + e.getMessage()));
            return 0;
        }

        int knowledgeStoreSize = baseVillager.getKnowledgeStore().size();
        int seedCount = batchRequest.getVillagers().get(0).getSeeds().size();
        String filename = outputPath.getFileName().toString();

        context.getSource().sendSuccess(() -> Component.literal(
                "[monologue dump] villager=" + baseVillager.getUUID()
                        + " | occasions=" + occasions.stream()
                        .map(o -> o.name().toLowerCase(Locale.ROOT))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("none")
                        + " | seeds=" + seedCount + "/" + knowledgeStoreSize
                        + " | file=" + filename), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * Resolves the occasions to include in the dump. Returns an empty Optional (after sending a
     * failure message) when the provided occasion string is not a recognized {@link Occasion}.
     */
    private static Optional<List<Occasion>> resolveRequestedOccasions(CommandContext<CommandSourceStack> context,
                                                                      BaseVillager villager) {
        String rawOccasion;
        try {
            rawOccasion = StringArgumentType.getString(context, ARG_OCCASION);
        } catch (IllegalArgumentException ignored) {
            // Argument was omitted — use the villager's live occasion
            return Optional.of(List.of(villager.getCurrentOccasion()));
        }

        if (OCCASION_ARG_ALL.equalsIgnoreCase(rawOccasion)) {
            return Optional.of(List.of(Occasion.values()));
        }

        try {
            return Optional.of(List.of(Occasion.valueOf(rawOccasion.toUpperCase(Locale.ROOT))));
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal(
                    "Unknown occasion '" + rawOccasion + "'. Use 'all' or one of: "
                            + Arrays.stream(Occasion.values())
                            .map(o -> o.name().toLowerCase(Locale.ROOT))
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")));
            return Optional.empty();
        }
    }

    private static String prettyPrint(String compactJson) {
        JsonElement element = JsonParser.parseString(compactJson);
        return PRETTY_GSON.toJson(element);
    }

    private static Path buildMonologueOutputPath() {
        Path outputDir = FMLPaths.GAMEDIR.get().resolve("settlements");
        String timestamp = LocalDateTime.now().format(GENERATION_TIMESTAMP_FORMAT);
        return outputDir.resolve("monologue_payload_" + timestamp + ".json");
    }

}
