package dev.breezes.settlements.presentation.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.generation.model.GenerationResult;
import dev.breezes.settlements.domain.generation.model.geometry.BlockPosition;
import dev.breezes.settlements.domain.generation.model.geometry.BoundingRegion;
import dev.breezes.settlements.domain.generation.model.profile.ScaleTier;
import dev.breezes.settlements.domain.generation.model.survey.SurveyBounds;
import dev.breezes.settlements.domain.generation.model.survey.TerrainGrid;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
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
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestCommand {

    private static final int GENERATION_SAMPLE_INTERVAL = 4;
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
                .then(Commands.literal("open_inventory").executes(TestCommand::openInventory))
                .executes(TestCommand::execute));
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
