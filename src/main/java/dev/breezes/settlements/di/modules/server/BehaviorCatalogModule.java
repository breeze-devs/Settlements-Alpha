package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedSpecies;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.feeding.FeedWolfBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.feeding.FeedWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing.WashWolfBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.washing.WashWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer.SurveyLandscapeBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cartographer.SurveyLandscapeConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship.CourtshipAcceptBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship.CourtshipInitiateBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship.CourtshipInitiateConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.courtship.CourtshipPresenter;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestMelonBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestMelonConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestNetherWartBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestNetherWartConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestPumpkinBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestPumpkinConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestRipeCropsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestRipeCropsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSweetBerriesBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSweetBerriesConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger.EatFoodBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.investigate.InvestigateBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.investigate.InvestigateConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.dyeleather.DyeLeatherBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.dyeleather.DyeLeatherConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.washleather.WashLeatherBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.leatherworking.washleather.WashLeatherConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.CollectDemandedItemBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.CollectDemandedItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.TakeFromChestBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.logistics.TakeFromChestConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.ChaseChickensBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.ChaseChickensConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.RingBellBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.RingBellConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.ThrowEggsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.ThrowEggsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.trading.PartnerScanner;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.trading.TradeAcceptBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.trading.TradeInitiateBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.trading.TradeSessionPresenter;
import dev.breezes.settlements.application.ai.courtship.BedReservationService;
import dev.breezes.settlements.application.ai.courtship.CourtshipChoreographyLibrary;
import dev.breezes.settlements.application.ai.courtship.CourtshipSelfMemoryRecorder;
import dev.breezes.settlements.application.ai.courtship.CourtshipSessionRegistry;
import dev.breezes.settlements.application.ai.targeting.BlockMemoryTargetResolver;
import dev.breezes.settlements.application.ai.trading.NegotiationEngine;
import dev.breezes.settlements.application.ai.trading.TradeExecutor;
import dev.breezes.settlements.application.ai.trading.TradeSessionRegistry;
import dev.breezes.settlements.application.ai.trading.TradingConfig;
import dev.breezes.settlements.application.economy.VillagerWallet;
import dev.breezes.settlements.application.economy.catalog.TradePriceResolver;
import dev.breezes.settlements.application.economy.demand.DemandEvaluator;
import dev.breezes.settlements.application.economy.demand.DemandSignalService;
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.application.hunger.HungerConfig;
import dev.breezes.settlements.di.catalog.BehaviorCatalogEntry;
import dev.breezes.settlements.domain.ai.catalog.BehaviorCategory;
import dev.breezes.settlements.domain.ai.catalog.BehaviorChannel;
import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.CooldownRange;
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.ai.credibility.ReputationQuery;
import dev.breezes.settlements.domain.ai.worldevent.WorldEventEmitter;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.shared.util.ReputationUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Items;

/**
 * Registers all behavior variants as {@link BehaviorCatalogEntry} Dagger multibindings.
 * <p>
 * One method per behavior variant. Parameterized variants that share an implementation class
 * (e.g. breed_pigs / breed_chickens / breed_cows all use {@link BreedAnimalsBehavior}) get
 * separate methods — the factory lambda closes over the species-specific config.
 * <p>
 * Which professions use which behaviors is NOT declared here. That mapping lives in {@link PoolModule}.
 * <p>
 * To add a new behavior: add one {@code @Provides @IntoSet} method here and a key constant
 * to {@link BehaviorKey}. Then add the key to the relevant profession pool(s) in {@link PoolModule}.
 */
@Module
public final class BehaviorCatalogModule {

    // =========================================================================
    // Universal
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry takeFromChest(TakeFromChestConfig config,
                                              HungerConfig hungerConfig,
                                              DemandEvaluator demandEvaluator) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.TAKE_FROM_CHEST)
                        .displayName("Fetch from Chest")
                        .description("Fetch a needed item from a nearby village chest")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(15).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.TAKE_FROM_CHEST.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("chest"))
                        .build())
                .factory(() -> new TakeFromChestBehavior(config, hungerConfig, demandEvaluator))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry collectDemandedItem(CollectDemandedItemConfig config,
                                                    HungerConfig hungerConfig,
                                                    DemandEvaluator demandEvaluator) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.COLLECT_DEMANDED_ITEM)
                        .displayName("Collect Items")
                        .description("Pick up a wanted item dropped nearby")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(15).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.COLLECT_DEMANDED_ITEM.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("hopper"))
                        .build())
                .factory(() -> new CollectDemandedItemBehavior(config, hungerConfig, demandEvaluator))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry eatFood(HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.EAT_FOOD)
                        .displayName("Eat Food")
                        .description("Consume food from inventory to restore hunger")
                        .category(BehaviorCategory.SELF_CARE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(5).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(1, 1))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.EAT_FOOD.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("bread"))
                        .build())
                .factory(() -> new EatFoodBehavior(hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tradeInitiate(TradingConfig config,
                                              HungerConfig hungerConfig,
                                              TradeSessionRegistry sessionRegistry,
                                              TradeCatalogRegistry tradeCatalogRegistry,
                                              TradePriceResolver tradePriceResolver,
                                              DemandSignalService demandSignalService,
                                              VillagerWallet villagerWallet,
                                              TradeExecutor tradeExecutor,
                                              TradeSessionPresenter tradeSessionPresenter,
                                              DemandEvaluator demandEvaluator,
                                              PartnerScanner partnerScanner,
                                              NegotiationEngine negotiationEngine,
                                              WorldEventEmitter worldEventEmitter) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.TRADE_INITIATE)
                        .displayName("Initiate Trade")
                        .description("Seek a nearby villager and propose a trade session")
                        .category(BehaviorCategory.SOCIAL)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .requiredChannel(BehaviorChannel.SOCIAL)
                        .estimatedDuration(ClockTicks.seconds(35).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.TRADE_INITIATE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("emerald"))
                        .build())
                .factory(() -> new TradeInitiateBehavior(config, hungerConfig, sessionRegistry, tradeCatalogRegistry,
                        tradePriceResolver, demandSignalService, villagerWallet, tradeExecutor,
                        tradeSessionPresenter, demandEvaluator, partnerScanner, negotiationEngine, worldEventEmitter))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tradeAccept(TradingConfig config,
                                            HungerConfig hungerConfig,
                                            TradeSessionRegistry sessionRegistry,
                                            TradeSessionPresenter tradeSessionPresenter) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.TRADE_ACCEPT)
                        .displayName("Accept Trade")
                        .description("Accept an incoming trade invitation from another villager")
                        .category(BehaviorCategory.SOCIAL)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .requiredChannel(BehaviorChannel.SOCIAL)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.acceptBehaviorCooldownSecondsMin(), config.acceptBehaviorCooldownSecondsMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.TRADE_ACCEPT.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("emerald"))
                        .build())
                .factory(() -> new TradeAcceptBehavior(config, hungerConfig, sessionRegistry, tradeSessionPresenter))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry courtshipInitiate(CourtshipInitiateConfig config,
                                                  HungerConfig hungerConfig,
                                                  CourtshipSessionRegistry courtshipSessionRegistry,
                                                  BedReservationService bedReservationService,
                                                  CourtshipPresenter courtshipPresenter,
                                                  CourtshipChoreographyLibrary choreographyLibrary,
                                                  WorldEventEmitter worldEventEmitter,
                                                  CourtshipSelfMemoryRecorder selfMemoryRecorder) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.COURTSHIP_INITIATE)
                        .displayName("Initiate Courtship")
                        .description("Seek a nearby willing villager and initiate courtship")
                        .category(BehaviorCategory.SOCIAL)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .requiredChannel(BehaviorChannel.SOCIAL)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.COURTSHIP_INITIATE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("poppy"))
                        .build())
                .factory(() -> new CourtshipInitiateBehavior(config, hungerConfig, courtshipSessionRegistry,
                        bedReservationService, courtshipPresenter, choreographyLibrary, worldEventEmitter, selfMemoryRecorder))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry courtshipAccept(HungerConfig hungerConfig,
                                                CourtshipSessionRegistry courtshipSessionRegistry,
                                                CourtshipPresenter courtshipPresenter,
                                                CourtshipChoreographyLibrary choreographyLibrary) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.COURTSHIP_ACCEPT)
                        .displayName("Accept Courtship")
                        .description("Accept an incoming courtship invitation from another villager")
                        .category(BehaviorCategory.SOCIAL)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .requiredChannel(BehaviorChannel.SOCIAL)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(1, 1))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.COURTSHIP_ACCEPT.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("poppy"))
                        .build())
                .factory(() -> new CourtshipAcceptBehavior(hungerConfig, courtshipSessionRegistry,
                        courtshipPresenter, choreographyLibrary))
                .build();
    }

    // =========================================================================
    // Village support
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry repairIronGolem(RepairIronGolemConfig config, HungerConfig hungerConfig,
                                                DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.REPAIR_IRON_GOLEM)
                        .displayName("Repair Iron Golem")
                        .description("Apply iron ingots to repair the village's iron golem")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.REPAIR_IRON_GOLEM.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_ingot"))
                        .build())
                .factory(() -> new RepairIronGolemBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    // =========================================================================
    // Smithing
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry blastOre(BlastOreConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BLAST_ORE)
                        .displayName("Blast Ore")
                        .description("Smelt ore in a blast furnace")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BLAST_ORE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("blast_furnace"))
                        .build())
                .factory(() -> new BlastOreBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Butchering
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry smokeMeat(SmokeMeatConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.SMOKE_MEAT)
                        .displayName("Smoke Meat")
                        .description("Cook raw meat in the smoker")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.SMOKE_MEAT.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("smoker"))
                        .build())
                .factory(() -> new SmokeMeatBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry butcherLivestock(ButcherLivestockConfig config, HungerConfig hungerConfig,
                                                 DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BUTCHER_LIVESTOCK)
                        .displayName("Butcher Livestock")
                        .description("Slaughter mature livestock for meat")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BUTCHER_LIVESTOCK.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_axe"))
                        .build())
                .factory(() -> new ButcherLivestockBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    // =========================================================================
    // Cleric
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry throwPotions(ThrowPotionsConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.THROW_POTIONS)
                        .displayName("Throw Potions")
                        .description("Throw splash potions at injured nearby villagers")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.THROW_POTIONS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("splash_potion"))
                        .build())
                .factory(() -> new ThrowPotionsBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestNetherWart(HarvestNetherWartConfig config,
                                                  HungerConfig hungerConfig,
                                                  BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_NETHER_WART)
                        .displayName("Harvest Nether Wart")
                        .description("Harvest mature nether wart from soul sand")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_NETHER_WART.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("nether_wart"))
                        .build())
                .factory(() -> new HarvestNetherWartBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    // =========================================================================
    // Farming
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestSugarCane(HarvestSugarCaneConfig config,
                                                 HungerConfig hungerConfig,
                                                 BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_SUGARCANE)
                        .displayName("Harvest Sugar Cane")
                        .description("Cut mature sugarcane from nearby fields")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_SUGARCANE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("sugar_cane"))
                        .build())
                .factory(() -> new HarvestSugarCaneBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry collectHoney(CollectHoneyConfig config,
                                             HungerConfig hungerConfig,
                                             CollectHoneyYieldDataManager yieldData,
                                             DemandSignalService demandSignalService,
                                             BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.COLLECT_HONEY)
                        .displayName("Collect Honey")
                        .description("Collect honey bottles from full beehives")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.COLLECT_HONEY.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("honey_bottle"))
                        .build())
                .factory(() -> new CollectHoneyBehavior(config, hungerConfig, yieldData, demandSignalService, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestHoneycomb(HarvestHoneycombConfig config,
                                                 HungerConfig hungerConfig,
                                                 HarvestHoneycombYieldDataManager yieldData,
                                                 DemandSignalService demandSignalService,
                                                 BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_HONEYCOMB)
                        .displayName("Harvest Honeycomb")
                        .description("Scrape honeycomb from full beehives using shears")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_HONEYCOMB.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("honeycomb"))
                        .build())
                .factory(() -> new HarvestHoneycombBehavior(config, hungerConfig, yieldData, demandSignalService, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestPumpkin(HarvestPumpkinConfig config,
                                               HungerConfig hungerConfig,
                                               BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_PUMPKIN)
                        .displayName("Harvest Pumpkin")
                        .description("Harvest ripe pumpkins from nearby stems")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_PUMPKIN.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("pumpkin"))
                        .build())
                .factory(() -> new HarvestPumpkinBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestMelon(HarvestMelonConfig config,
                                             HungerConfig hungerConfig,
                                             BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_MELON)
                        .displayName("Harvest Melon")
                        .description("Harvest ripe melons from nearby stems")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_MELON.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("melon"))
                        .build())
                .factory(() -> new HarvestMelonBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestSweetBerries(HarvestSweetBerriesConfig config,
                                                    HungerConfig hungerConfig,
                                                    BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_SWEET_BERRIES)
                        .displayName("Harvest Sweet Berries")
                        .description("Pick ripe sweet berries from nearby bushes")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_SWEET_BERRIES.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("sweet_berries"))
                        .build())
                .factory(() -> new HarvestSweetBerriesBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestRipeCrops(HarvestRipeCropsConfig config,
                                                 HungerConfig hungerConfig,
                                                 BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_RIPE_CROPS)
                        .displayName("Harvest Ripe Crops")
                        .description("Harvest ripe crops from nearby farmland")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_RIPE_CROPS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("wheat"))
                        .build())
                .factory(() -> new HarvestRipeCropsBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    // =========================================================================
    // Animal handling
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry milkCow(MilkCowConfig config, HungerConfig hungerConfig,
                                        DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.MILK_COW)
                        .displayName("Milk Cow")
                        .description("Milk nearby cows using a bucket")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.MILK_COW.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("milk_bucket"))
                        .build())
                .factory(() -> new MilkCowBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedChickens(BreedAnimalsConfig config, HungerConfig hungerConfig,
                                              DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BREED_CHICKENS)
                        .displayName("Breed Chickens")
                        .description("Feed and breed nearby chickens to grow the flock")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BREED_CHICKENS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("chicken_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, BreedSpecies.builder()
                        .type(EntityType.CHICKEN)
                        .foodTag(ItemTags.CHICKEN_FOOD)
                        .canonicalFood(Items.WHEAT_SEEDS)
                        .build(), demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedCows(BreedAnimalsConfig config, HungerConfig hungerConfig,
                                          DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BREED_COWS)
                        .displayName("Breed Cows")
                        .description("Feed and breed nearby cows to grow the herd")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BREED_COWS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("cow_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, BreedSpecies.builder()
                        .type(EntityType.COW)
                        .foodTag(ItemTags.COW_FOOD)
                        .canonicalFood(Items.WHEAT)
                        .build(), demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedPigs(BreedAnimalsConfig config, HungerConfig hungerConfig,
                                          DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BREED_PIGS)
                        .displayName("Breed Pigs")
                        .description("Feed and breed nearby pigs to grow the herd")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BREED_PIGS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("pig_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, BreedSpecies.builder()
                        .type(EntityType.PIG)
                        .foodTag(ItemTags.PIG_FOOD)
                        .canonicalFood(Items.CARROT)
                        .build(), demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedSheep(BreedAnimalsConfig config, HungerConfig hungerConfig,
                                           DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.BREED_SHEEP)
                        .displayName("Breed Sheep")
                        .description("Feed and breed nearby sheep to grow the herd")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.BREED_SHEEP.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("sheep_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, BreedSpecies.builder()
                        .type(EntityType.SHEEP)
                        .foodTag(ItemTags.SHEEP_FOOD)
                        .canonicalFood(Items.WHEAT)
                        .build(), demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tameWolf(TameWolfConfig config, HungerConfig hungerConfig,
                                         DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.TAME_WOLF)
                        .displayName("Tame Wolf")
                        .description("Offer bones to a nearby wild wolf to tame it")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.TAME_WOLF.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("bone"))
                        .build())
                .factory(() -> new TameWolfBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tameCat(TameCatConfig config, HungerConfig hungerConfig,
                                        DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.TAME_CAT)
                        .displayName("Tame Cat")
                        .description("Offer raw fish to a nearby stray cat to tame it")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.TAME_CAT.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("cod"))
                        .build())
                .factory(() -> new TameCatBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry shearSheep(ShearSheepConfig config,
                                           HungerConfig hungerConfig,
                                           DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.SHEAR_SHEEP)
                        .displayName("Shear Sheep")
                        .description("Shear wool from nearby sheep using shears")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.SHEAR_SHEEP.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("shears"))
                        .build())
                .factory(() -> new ShearSheepBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry feedWolf(FeedWolfConfig config, HungerConfig hungerConfig,
                                         DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.FEED_WOLF)
                        .displayName("Feed Wolf")
                        .description("Offer meat to a nearby owned wolf to restore its health")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(10).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.FEED_WOLF.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("cooked_beef"))
                        .build())
                .factory(() -> new FeedWolfBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry washWolf(WashWolfConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.WASH_WOLF)
                        .displayName("Wash Dog")
                        .description("Wash and dry an owned dog")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(10).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.WASH_WOLF.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("water_bucket"))
                        .build())
                .factory(() -> new WashWolfBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Cartographer
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry surveyLandscape(SurveyLandscapeConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.SURVEY_LANDSCAPE)
                        .displayName("Survey Landscape")
                        .description("Travel to distant points and survey the landscape")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.SURVEY_LANDSCAPE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("map"))
                        .build())
                .factory(() -> new SurveyLandscapeBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Fishing
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry fishing(FishingConfig config, HungerConfig hungerConfig,
                                        DemandSignalService demandSignalService) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.FISHING)
                        .displayName("Go Fishing")
                        .description("Cast a fishing rod and wait for a catch")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.FISHING.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("fishing_rod"))
                        .build())
                .factory(() -> new FishingBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    // =========================================================================
    // Crafting / Mason
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry cutStone(CutStoneConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.CUT_STONE)
                        .displayName("Cut Stone")
                        .description("Shape stone blocks at the stonecutter")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.CUT_STONE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("stonecutter"))
                        .build())
                .factory(() -> new CutStoneBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestOre(HarvestOreConfig config,
                                           HungerConfig hungerConfig,
                                           BlockMemoryTargetResolver targetResolver) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_ORE)
                        .displayName("Harvest Ore")
                        .description("Mine ore blocks from a nearby vein")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.HARVEST_ORE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_ore"))
                        .build())
                .factory(() -> new HarvestOreBehavior(config, hungerConfig, targetResolver))
                .build();
    }

    // =========================================================================
    // Enchanting
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry enchantItem(EnchantItemConfig config,
                                            HungerConfig hungerConfig,
                                            EnchantmentEngine enchantmentEngine) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.ENCHANT_ITEM)
                        .displayName("Enchant Item")
                        .description("Apply enchantments to an item at the enchanting table")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.ENCHANT_ITEM.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("enchanting_table"))
                        .build())
                .factory(() -> new EnchantItemBehavior(config, hungerConfig, enchantmentEngine))
                .build();
    }

    // =========================================================================
    // Idle / Leisure
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry walkDog(WalkDogConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.WALK_DOG)
                        .displayName("Walk Dog")
                        .description("Take a tamed wolf for a leisurely walk around the village")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .estimatedDuration(ClockTicks.seconds(60).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.WALK_DOG.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("lead"))
                        .build())
                .factory(() -> new WalkDogBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Leatherworking
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry dyeLeather(DyeLeatherConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.DYE_LEATHER)
                        .displayName("Dye Leather")
                        .description("Dye leather armor on an armor stand")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.DYE_LEATHER.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("leather_chestplate"))
                        .build())
                .factory(() -> new DyeLeatherBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry washLeather(WashLeatherConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.WASH_LEATHER)
                        .displayName("Wash Leather")
                        .description("Wash leather in the cauldron")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.WASH_LEATHER.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("leather"))
                        .build())
                .factory(() -> new WashLeatherBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Nitwit / Mischief
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry ringBell(RingBellConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.RING_BELL)
                        .displayName("Ring Bell")
                        .description("Ring a nearby village bell for no productive reason")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(20).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.RING_BELL.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("bell"))
                        .build())
                .factory(() -> new RingBellBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry throwEggs(ThrowEggsConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.THROW_EGGS)
                        .displayName("Throw Eggs")
                        .description("Pelt nearby villagers, players, and wandering traders with eggs")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.THROW_EGGS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("egg"))
                        .build())
                .factory(() -> new ThrowEggsBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry chaseChickens(ChaseChickensConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.CHASE_CHICKENS)
                        .displayName("Chase Chickens")
                        .description("Chase a chicken around the village")
                        .category(BehaviorCategory.LEISURE)
                        .intensity(WorkIntensity.NONE)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.CHASE_CHICKENS.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("feather"))
                        .build())
                .factory(() -> new ChaseChickensBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Investigate
    // =========================================================================

    /**
     * Investigate: navigate to a hearsay tip location, force a sensor scan, evaluate the
     * claim predicate, and resolve the entry CONFIRMED or REFUTED.
     * <p>
     * The behavior is registered as a normal catalog entry so the planner and override lane
     * can retrieve it via {@code IBehaviorCatalog.createBehavior(BehaviorKey.INVESTIGATE)}.
     * The behavior self-selects its tip via InvestigateTipSelector inside its precondition check,
     * so no external configure() call is needed before tickPreconditions().
     */
    @Provides
    @IntoSet
    static BehaviorCatalogEntry investigate(InvestigateConfig config,
                                            HungerConfig hungerConfig,
                                            ReputationUtil reputationUtil,
                                            ReputationQuery reputationQuery) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.INVESTIGATE)
                        .displayName("Investigate Tip")
                        .description("Travel to a hearsay tip location and verify the claim")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.LIGHT)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(45).asGameTicks())
                        .cooldown(CooldownRange.ofSeconds(config.behaviorCooldownMin(), config.behaviorCooldownMax()))
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey(BehaviorKey.INVESTIGATE.displayNameKey())
                        .iconItemId(ResourceLocation.withDefaultNamespace("compass"))
                        .build())
                .factory(() -> new InvestigateBehavior(config, hungerConfig, reputationUtil, reputationQuery))
                .build();
    }

}
