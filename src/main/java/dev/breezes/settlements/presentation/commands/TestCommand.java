package dev.breezes.settlements.presentation.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.application.ui.bubble.BubbleChannel;
import dev.breezes.settlements.application.ui.bubble.BubbleCommand;
import dev.breezes.settlements.application.ui.bubble.BubbleEntry;
import dev.breezes.settlements.application.ui.bubble.BubbleMessage;
import dev.breezes.settlements.application.ui.bubble.BubbleSegment;
import dev.breezes.settlements.application.ui.bubble.SpriteRef;
import dev.breezes.settlements.application.ui.bubble.TradeMarker;
import dev.breezes.settlements.application.ui.bubble.VillagerBubbleService;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.settlement.query.BuildingContext;
import dev.breezes.settlements.domain.settlement.query.SettlementPositionContext;
import dev.breezes.settlements.domain.settlement.query.SettlementQueryService;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.generation.debug.GenerationResultSerializer;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.infrastructure.minecraft.worldgen.TerrainGridFactory;
import dev.breezes.settlements.shared.util.CoordinateHashUtil;
import dev.breezes.settlements.shared.util.VillagerRaycastUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestCommand {

    private static final int GENERATION_SAMPLE_INTERVAL = 4;

    // Argument names for the bubble subcommand
    private static final String ARG_ITEM_ID = "item_id";
    private static final String ARG_COUNT = "count";
    private static final String ARG_EMERALD_COUNT = "emerald_count";
    private static final String ARG_MARKER = "marker";
    private static final String ARG_CHANNEL = "channel";
    private static final String ARG_OWNER_KEY = "owner_key";
    private static final String ARG_BUBBLE_ID = "bubble_id";

    private static final Ticks TEST_BUBBLE_TTL = Ticks.seconds(10);
    private static final int GENERATION_SURVEY_PADDING = 30;
    private static final DateTimeFormatter GENERATION_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final Map<String, ResourceLocation> STRUCTURE_TEMPLATES = Map.ofEntries(
            Map.entry("town_hall", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/town_hall")),
            Map.entry("tavern", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/tavern")),
            Map.entry("market_stall", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/market_stall")),
            Map.entry("house", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/house")),
            Map.entry("sawmill", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/sawmill")),
            Map.entry("lumber_camp", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/lumber_camp")),
            Map.entry("farm", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/farm")),
            Map.entry("wheat_field", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/wheat_field")),
            Map.entry("fishing_dock", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/fishing_dock")),
            Map.entry("fish_market", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/fish_market")),
            Map.entry("mine", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/mine")),
            Map.entry("stonecutter", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/stonecutter")),
            Map.entry("quarry", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/quarry")),
            Map.entry("watchtower", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/watchtower")),
            Map.entry("barracks", ResourceLocation.fromNamespaceAndPath(SettlementsMod.MOD_ID, "buildings/barracks"))
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stest")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("place")
                        .then(Commands.argument("structure", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    STRUCTURE_TEMPLATES.keySet().forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("rotation", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("none");
                                            builder.suggest("clockwise_90");
                                            builder.suggest("clockwise_180");
                                            builder.suggest("counterclockwise_90");
                                            return builder.buildFuture();
                                        })
                                        .executes(TestCommand::placeStructure))))
                .then(Commands.literal("generate")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                        .executes(TestCommand::generateSettlement)
                                        .then(Commands.argument("scale", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("hamlet");
                                                    builder.suggest("village");
                                                    builder.suggest("town");
                                                    return builder.buildFuture();
                                                })
                                                .executes(TestCommand::generateSettlement)
                                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                                        .executes(TestCommand::generateSettlement))))))
                .then(Commands.literal("info").executes(TestCommand::settlementInfo))
                .then(Commands.literal("open_inventory").executes(TestCommand::openInventory))
                .then(buildBubbleCommand()));
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

        var settlementMetadata = positionContext.settlement().orElseThrow().metadata();
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
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildBubbleCommand() {
        return Commands.literal("bubble")
                .then(buildPushBubbleCommand())
                .then(buildUpsertBubbleCommand())
                .then(Commands.literal("remove_by_id")
                        .then(Commands.argument(ARG_BUBBLE_ID, StringArgumentType.word())
                                .executes(TestCommand::removeBubbleById)))
                .then(Commands.literal("remove_by_owner")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .then(Commands.argument(ARG_OWNER_KEY, StringArgumentType.word())
                                        .executes(TestCommand::removeBubbleByOwner))))
                .then(Commands.literal("clear_channel")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .executes(TestCommand::clearBubbleChannel)))
                .executes(TestCommand::execute);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPushBubbleCommand() {
        return Commands.literal("push")
                .then(Commands.literal("trade_negotiation")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .then(Commands.argument(ARG_ITEM_ID, StringArgumentType.word())
                                        .then(Commands.argument(ARG_COUNT, IntegerArgumentType.integer(0))
                                                .then(Commands.argument(ARG_EMERALD_COUNT, IntegerArgumentType.integer(0))
                                                        .executes(TestCommand::pushTradeNegotiationBubble)
                                                        .then(Commands.argument(ARG_MARKER, StringArgumentType.word())
                                                                .suggests(TestCommand::suggestMarkers)
                                                                .executes(TestCommand::pushTradeNegotiationBubble)))))))
                .then(Commands.literal("shear_sheep")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .executes(TestCommand::pushShearSheepBubble)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildUpsertBubbleCommand() {
        return Commands.literal("upsert")
                .then(Commands.literal("trade_negotiation")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .then(Commands.argument(ARG_OWNER_KEY, StringArgumentType.word())
                                        .then(Commands.argument(ARG_ITEM_ID, StringArgumentType.word())
                                                .then(Commands.argument(ARG_COUNT, IntegerArgumentType.integer(1))
                                                        .then(Commands.argument(ARG_EMERALD_COUNT, IntegerArgumentType.integer(1))
                                                                .executes(TestCommand::upsertTradeNegotiationBubble)
                                                                .then(Commands.argument(ARG_MARKER, StringArgumentType.word())
                                                                        .suggests(TestCommand::suggestMarkers)
                                                                        .executes(TestCommand::upsertTradeNegotiationBubble))))))))
                .then(Commands.literal("shear_sheep")
                        .then(Commands.argument(ARG_CHANNEL, StringArgumentType.word())
                                .suggests(TestCommand::suggestChannels)
                                .then(Commands.argument(ARG_OWNER_KEY, StringArgumentType.word())
                                        .executes(TestCommand::upsertShearSheepBubble))));
    }

    private static int generateSettlement(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();

        int x = IntegerArgumentType.getInteger(context, "x");
        int z = IntegerArgumentType.getInteger(context, "z");
        ScaleTier requestedScale = parseScale(context);
        long seed = parseSeed(context, level, x, z);

        SurveyBounds bounds = buildSurveyBounds(x, z, requestedScale);
        TerrainGrid terrainGrid = TerrainGridFactory.fromServerLevel(level, bounds, GENERATION_SAMPLE_INTERVAL);

        GenerationResult result = SettlementsDagger.component().generationPipeline().generate(terrainGrid, bounds, seed);

        try {
            Path outputFile = buildGenerationOutputPath();
            GenerationResultSerializer.writeToFile(result, outputFile);
            source.sendSuccess(() -> Component.literal(
                    "Generation exported to " + outputFile.getFileName()
                            + " | requestedScale=" + requestedScale.name().toLowerCase(Locale.ROOT)
                            + " | sampledScale=" + result.profile().scaleTier().name().toLowerCase(Locale.ROOT)
                            + " | seed=" + seed), false);
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to export generation JSON: " + e.getMessage()));
            return 0;
        }
    }

    private static int placeStructure(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return 0;
        }

        String structureName = StringArgumentType.getString(context, "structure");
        ResourceLocation templateId = STRUCTURE_TEMPLATES.get(structureName);
        if (templateId == null) {
            context.getSource().sendFailure(Component.literal("Unknown structure: " + structureName));
            return 0;
        }

        Rotation rotation;
        try {
            rotation = parseRotation(StringArgumentType.getString(context, "rotation"));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        StructureTemplateManager structureManager = level.getServer().getStructureManager();
        Optional<StructureTemplate> templateOptional = structureManager.get(templateId);
        if (templateOptional.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "Could not find structure template for '" + structureName + "': " + templateId));
            return 0;
        }

        StructureTemplate template = templateOptional.get();
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(rotation)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(false);

        var origin = player.blockPosition();
        var placementOrigin = template.getZeroPositionWithTransform(origin, Mirror.NONE, rotation);
        boolean placed = template.placeInWorld(level, placementOrigin, placementOrigin, settings, level.random, Block.UPDATE_ALL);
        if (!placed) {
            context.getSource().sendFailure(Component.literal(
                    "Failed to place structure '" + structureName + "' at " + placementOrigin +
                            " size=" + template.getSize() +
                            " id=" + templateId));
            return 0;
        }

        player.displayClientMessage(Component.literal(
                "Placed structure '" + structureName + "' at " + placementOrigin +
                        " size=" + template.getSize() +
                        " rotation=" + rotation.name().toLowerCase(Locale.ROOT)), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int openInventory(CommandContext<CommandSourceStack> command) {
        if (!(command.getSource().getEntity() instanceof Player player)) {
            return Command.SINGLE_SUCCESS;
        }

        Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(player, 15.0);

        if (hitResult.isEmpty() || !(hitResult.get().getEntity() instanceof ISettlementsVillager villager)) {
            player.displayClientMessage(Component.literal("No villager found in range"), true);
            return Command.SINGLE_SUCCESS;
        }

        player.displayClientMessage(Component.literal("Opening inventory for villager: " + villager.getUUID()),
                true);

        VillagerInventory settlementsInventory = villager.getSettlementsInventory();
        SimpleContainer realBackpack = settlementsInventory.getBackpack();
        int realSize = settlementsInventory.getBackpackSize();
        SimpleContainer virtualContainer = new SimpleContainer(54);

        // Initialize virtual container
        for (int i = 0; i < 54; i++) {
            if (i < realSize) {
                virtualContainer.setItem(i, realBackpack.getItem(i));
            } else {
                ItemStack barrier = new ItemStack(Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Locked Slot"));
                virtualContainer.setItem(i, barrier);
            }
        }

        player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                new ChestMenu(MenuType.GENERIC_9x6, id, inv, virtualContainer, 6),
                Component.literal("Villager Inventory")));

        return Command.SINGLE_SUCCESS;
    }

    private static int pushTradeNegotiationBubble(CommandContext<CommandSourceStack> context) {
        return applyPresetBubbleCommand(context, false, BubblePreset.TRADE_NEGOTIATION);
    }

    private static int upsertTradeNegotiationBubble(CommandContext<CommandSourceStack> context) {
        return applyPresetBubbleCommand(context, true, BubblePreset.TRADE_NEGOTIATION);
    }

    private static int pushShearSheepBubble(CommandContext<CommandSourceStack> context) {
        return applyPresetBubbleCommand(context, false, BubblePreset.SHEAR_SHEEP);
    }

    private static int upsertShearSheepBubble(CommandContext<CommandSourceStack> context) {
        return applyPresetBubbleCommand(context, true, BubblePreset.SHEAR_SHEEP);
    }

    private static int applyPresetBubbleCommand(CommandContext<CommandSourceStack> context,
                                                boolean upsert,
                                                BubblePreset preset) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }

        BubbleChannel channel;
        try {
            channel = parseChannel(StringArgumentType.getString(context, ARG_CHANNEL));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }

        BubbleMessage message;
        try {
            message = buildBubbleMessage(context, preset);
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }

        ISettlementsVillager villager = villagerOptional.get();
        long gameTime = villager.getMinecraftEntity().level().getGameTime();
        VillagerBubbleService bubbleService = SettlementsDagger.serverOrThrow().villagerBubbleService();

        BubbleCommand command;
        String operation;
        if (upsert) {
            String ownerKey = StringArgumentType.getString(context, ARG_OWNER_KEY);
            command = new BubbleCommand.Upsert(channel, ownerKey, message);
            operation = "upserted";
        } else {
            command = new BubbleCommand.Push(channel, message);
            operation = "pushed";
        }

        boolean changed = bubbleService.applyCommand(villager, command, gameTime);
        if (!changed) {
            context.getSource().sendFailure(Component.literal("Bubble command was rejected by channel policy."));
            return 0;
        }

        String ownerSuffix = upsert ? " | ownerKey=" + StringArgumentType.getString(context, ARG_OWNER_KEY) : "";
        Component successMessage = Component.literal(
                "Bubble " + operation + " on villager " + villager.getUUID()
                        + " | channel=" + channel.name()
                        + ownerSuffix
                        + " | sourceType=" + message.getSourceType()
                        + " | segments=" + message.getSegments());
        context.getSource().sendSuccess(() -> successMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeBubbleById(CommandContext<CommandSourceStack> context) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }

        UUID bubbleId;
        try {
            bubbleId = UUID.fromString(StringArgumentType.getString(context, ARG_BUBBLE_ID));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal("Invalid bubble UUID: "
                    + StringArgumentType.getString(context, ARG_BUBBLE_ID)));
            return 0;
        }

        ISettlementsVillager villager = villagerOptional.get();
        Optional<BubbleEntry> existing = villager.getBubbleState().getById(bubbleId);
        if (existing.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No bubble with id " + bubbleId + " exists on the targeted villager."));
            return 0;
        }

        VillagerBubbleService bubbleService = SettlementsDagger.serverOrThrow().villagerBubbleService();
        boolean changed = bubbleService.applyCommand(
                villager,
                new BubbleCommand.RemoveById(bubbleId),
                villager.getMinecraftEntity().level().getGameTime());

        if (!changed) {
            context.getSource().sendFailure(Component.literal("Failed to remove bubble " + bubbleId + "."));
            return 0;
        }

        var removed = existing.get();
        Component successMessage = Component.literal(
                "Removed bubble " + bubbleId
                        + " | villager=" + villager.getUUID()
                        + " | channel=" + removed.channel().name()
                        + " | ownerKey=" + removed.ownerKey()
                        + " | sourceType=" + removed.message().getSourceType());
        context.getSource().sendSuccess(() -> successMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int removeBubbleByOwner(CommandContext<CommandSourceStack> context) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }

        BubbleChannel channel;
        try {
            channel = parseChannel(StringArgumentType.getString(context, ARG_CHANNEL));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }

        String ownerKey = StringArgumentType.getString(context, ARG_OWNER_KEY);
        ISettlementsVillager villager = villagerOptional.get();
        Optional<BubbleEntry> existing =
                villager.getBubbleState().getByOwner(channel, ownerKey);
        if (existing.isEmpty()) {
            context.getSource().sendFailure(Component.literal(
                    "No bubble exists for owner '" + ownerKey + "' on channel " + channel.name() + "."));
            return 0;
        }

        VillagerBubbleService bubbleService = SettlementsDagger.serverOrThrow().villagerBubbleService();
        boolean changed = bubbleService.applyCommand(
                villager,
                new BubbleCommand.RemoveByOwner(channel, ownerKey),
                villager.getMinecraftEntity().level().getGameTime());

        if (!changed) {
            context.getSource().sendFailure(Component.literal("Failed to remove bubble for owner '" + ownerKey + "'."));
            return 0;
        }

        var removed = existing.get();
        Component successMessage = Component.literal(
                "Removed bubble by owner"
                        + " | villager=" + villager.getUUID()
                        + " | channel=" + channel.name()
                        + " | ownerKey=" + ownerKey
                        + " | bubbleId=" + removed.bubbleId()
                        + " | sourceType=" + removed.message().getSourceType());
        context.getSource().sendSuccess(() -> successMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearBubbleChannel(CommandContext<CommandSourceStack> context) {
        Optional<ISettlementsVillager> villagerOptional = getLookedAtVillager(context);
        if (villagerOptional.isEmpty()) {
            return 0;
        }

        BubbleChannel channel;
        try {
            channel = parseChannel(StringArgumentType.getString(context, ARG_CHANNEL));
        } catch (IllegalArgumentException ex) {
            context.getSource().sendFailure(Component.literal(ex.getMessage()));
            return 0;
        }

        ISettlementsVillager villager = villagerOptional.get();
        int removedCount = villager.getBubbleState().getEntries(channel).size();
        if (removedCount == 0) {
            context.getSource().sendFailure(Component.literal(
                    "No bubbles to clear on channel " + channel.name() + " for the targeted villager."));
            return 0;
        }

        VillagerBubbleService bubbleService = SettlementsDagger.serverOrThrow().villagerBubbleService();
        boolean changed = bubbleService.applyCommand(
                villager,
                new BubbleCommand.ClearChannel(channel),
                villager.getMinecraftEntity().level().getGameTime());

        if (!changed) {
            context.getSource().sendFailure(Component.literal("Failed to clear channel " + channel.name() + "."));
            return 0;
        }

        Component successMessage = Component.literal(
                "Cleared bubble channel " + channel.name()
                        + " | villager=" + villager.getUUID()
                        + " | removed=" + removedCount);
        context.getSource().sendSuccess(() -> successMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player player) {
            player.displayClientMessage(Component.literal("Starting test"), true);

            try {
                test(player);
            } catch (Exception e) {
                e.printStackTrace();
            }

            player.displayClientMessage(Component.literal("Ending test"), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void test(@Nonnull Player player) {
        // This is a test method
        TransformedBlockDisplay display = TransformedBlockDisplay.builder()
                .blockState(Blocks.SMOOTH_STONE.defaultBlockState())
                .transform(new TransformationMatrix(
                        0.8000f, 0.0000f, 0.0000f, -0.4000f,
                        0.0000f, 0.8000f, 0.0000f, 0.1000f,
                        0.0000f, 0.0000f, 0.8000f, -0.4000f,
                        0.0000f, 0.0000f, 0.0000f, 1.0000f))
                .build();
        Display displayEntity = display.spawn(Location.fromEntity(player, true));

        // Schedule a delayed run
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(() -> {
            display.setTransformation(new TransformationMatrix(
                    2.6000f, 0.0000f, 0.0000f, 0.1000f,
                    0.0000f, 0.5657f, -0.5657f, 0.1000f,
                    0.0000f, 0.5657f, 0.5657f, 0.1000f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f), Ticks.seconds(2));
        }, 2 * 1000, TimeUnit.MILLISECONDS);

        executor.schedule(display::remove, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Reads the optional {@code marker} argument without throwing if it was omitted.
     * Brigadier throws {@link IllegalArgumentException} for missing optional args, so we catch silently.
     */
    @Nullable
    private static String parseOptionalMarker(CommandContext<CommandSourceStack> context) {
        try {
            return StringArgumentType.getString(context, ARG_MARKER);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static BubbleMessage buildBubbleMessage(CommandContext<CommandSourceStack> context, BubblePreset preset) {
        return switch (preset) {
            case TRADE_NEGOTIATION -> BubbleMessage.builder()
                    .ttl(TEST_BUBBLE_TTL)
                    .sourceType("stest.trade_negotiation")
                    .segments(buildTradeNegotiationSegments(context))
                    .build();
            case SHEAR_SHEEP -> BubbleMessage.builder()
                    .ttl(TEST_BUBBLE_TTL)
                    .sourceType("stest.shear_sheep")
                    .segments(List.of(
                            BubbleSegment.Sprite.builder()
                                    .sprite(SpriteRef.SHEARS)
                                    .frameDuration(Ticks.seconds(0.5))
                                    .build(),
                            BubbleSegment.Sprite.builder()
                                    .sprite(SpriteRef.SHEEP)
                                    .frameDuration(Ticks.seconds(0.6))
                                    .build()))
                    .build();
        };
    }

    private static List<BubbleSegment> buildTradeNegotiationSegments(CommandContext<CommandSourceStack> context) {
        String rawItemId = StringArgumentType.getString(context, ARG_ITEM_ID);
        int itemCount = IntegerArgumentType.getInteger(context, ARG_COUNT);
        int emeraldCount = IntegerArgumentType.getInteger(context, ARG_EMERALD_COUNT);
        String markerName = parseOptionalMarker(context);

        ResourceLocation itemId;
        try {
            itemId = ResourceLocation.parse(rawItemId);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid item id: " + rawItemId);
        }

        List<BubbleSegment> segments = new ArrayList<>();
        segments.add(BubbleSegment.Item.builder()
                .itemId(itemId)
                .count(itemCount)
                .build());
        segments.add(BubbleSegment.Item.builder()
                .itemId(ResourceLocation.withDefaultNamespace("emerald"))
                .count(emeraldCount)
                .build());
        if (markerName != null) {
            segments.add(TradeMarker.fromSerializedName(markerName).asSegment());
        }
        return List.copyOf(segments);
    }

    private static BubbleChannel parseChannel(String rawChannel) {
        return switch (rawChannel.toLowerCase(Locale.ROOT)) {
            case "behavior" -> BubbleChannel.BEHAVIOR;
            case "chat" -> BubbleChannel.CHAT;
            case "system" -> BubbleChannel.SYSTEM;
            default -> throw new IllegalArgumentException("Unsupported bubble channel: " + rawChannel);
        };
    }

    private static CompletableFuture<Suggestions> suggestChannels(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        builder.suggest("behavior");
        builder.suggest("chat");
        builder.suggest("system");
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestMarkers(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        builder.suggest("up");
        builder.suggest("down");
        builder.suggest("check");
        builder.suggest("cross");
        return builder.buildFuture();
    }

    private enum BubblePreset {
        TRADE_NEGOTIATION,
        SHEAR_SHEEP,
    }

    private static Optional<ISettlementsVillager> getLookedAtVillager(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof Player player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command."));
            return Optional.empty();
        }

        Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(player, 15.0);
        if (hitResult.isEmpty() || !(hitResult.get().getEntity() instanceof ISettlementsVillager villager)) {
            player.displayClientMessage(Component.literal("No villager found in range"), true);
            return Optional.empty();
        }

        return Optional.of(villager);
    }

    private static Rotation parseRotation(String rawRotation) {
        return switch (rawRotation.toLowerCase(Locale.ROOT)) {
            case "none" -> Rotation.NONE;
            case "clockwise_90" -> Rotation.CLOCKWISE_90;
            case "clockwise_180" -> Rotation.CLOCKWISE_180;
            case "counterclockwise_90" -> Rotation.COUNTERCLOCKWISE_90;
            default -> throw new IllegalArgumentException("Unsupported rotation: " + rawRotation);
        };
    }

    private static ScaleTier parseScale(CommandContext<CommandSourceStack> context) {
        try {
            String rawScale = StringArgumentType.getString(context, "scale");
            return switch (rawScale.toLowerCase(Locale.ROOT)) {
                case "hamlet" -> ScaleTier.HAMLET;
                case "village" -> ScaleTier.VILLAGE;
                case "town" -> ScaleTier.TOWN;
                default -> throw new IllegalArgumentException("Unsupported scale: " + rawScale);
            };
        } catch (IllegalArgumentException ignored) {
            return ScaleTier.VILLAGE;
        }
    }

    private static long parseSeed(CommandContext<CommandSourceStack> context, ServerLevel level, int x, int z) {
        try {
            return LongArgumentType.getLong(context, "seed");
        } catch (IllegalArgumentException ignored) {
            return level.getSeed() ^ CoordinateHashUtil.hash(x, z);
        }
    }

    private static SurveyBounds buildSurveyBounds(int x, int z, ScaleTier scaleTier) {
        int radius = scaleTier.areaRadius();
        BlockPosition center = new BlockPosition(x, 0, z);
        BoundingRegion buildArea = new BoundingRegion(
                center.offset(-radius, 0, -radius),
                center.offset(radius, 0, radius)
        );
        return SurveyBounds.fromBuildArea(buildArea, GENERATION_SURVEY_PADDING);
    }

    private static Path buildGenerationOutputPath() {
        Path outputDir = FMLPaths.GAMEDIR.get().resolve("settlements");
        String timestamp = LocalDateTime.now().format(GENERATION_TIMESTAMP_FORMAT);
        return outputDir.resolve("generation_" + timestamp + ".json");
    }

}
