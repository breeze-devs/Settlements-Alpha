package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.butchering.ButcherLivestockConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.milking.MilkCowConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.cooking.smokemeat.SmokeMeatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.crafting.CutStoneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.enchanting.EnchantItemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.CollectHoneyConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestHoneycombConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSoulSandBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSoulSandConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.farming.HarvestSugarCaneConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.fishing.FishingConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.hunger.EatFoodBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.idle.WalkDogConfig;
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
import dev.breezes.settlements.domain.ai.catalog.WorkIntensity;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Set;

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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.eat_food")
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
                                              NegotiationEngine negotiationEngine) {
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.trade_initiate")
                        .iconItemId(ResourceLocation.withDefaultNamespace("emerald"))
                        .build())
                .factory(() -> new TradeInitiateBehavior(config, hungerConfig, sessionRegistry, tradeCatalogRegistry,
                        tradePriceResolver, demandSignalService, villagerWallet, tradeExecutor,
                        tradeSessionPresenter, demandEvaluator, partnerScanner, negotiationEngine))
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.trade_accept")
                        .iconItemId(ResourceLocation.withDefaultNamespace("emerald"))
                        .build())
                .factory(() -> new TradeAcceptBehavior(config, hungerConfig, sessionRegistry, tradeSessionPresenter))
                .build();
    }

    // =========================================================================
    // Village support
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry repairIronGolem(RepairIronGolemConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.repair_iron_golem")
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_ingot"))
                        .build())
                .factory(() -> new RepairIronGolemBehavior(config, hungerConfig))
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.blast_ore")
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
    static BehaviorCatalogEntry breedPigs(BreedAnimalsConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.breed_pigs")
                        .iconItemId(ResourceLocation.withDefaultNamespace("pig_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.PIG)))
                .build();
    }

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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.smoke_meat")
                        .iconItemId(ResourceLocation.withDefaultNamespace("smoker"))
                        .build())
                .factory(() -> new SmokeMeatBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry butcherLivestock(ButcherLivestockConfig config, HungerConfig hungerConfig) {
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
                        .estimatedDuration(ClockTicks.seconds(30).asGameTicks())
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.butcher_livestock")
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_axe"))
                        .build())
                .factory(() -> new ButcherLivestockBehavior(config, hungerConfig))
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.throw_potions")
                        .iconItemId(ResourceLocation.withDefaultNamespace("splash_potion"))
                        .build())
                .factory(() -> new ThrowPotionsBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestSoulSand(HarvestSoulSandConfig config, HungerConfig hungerConfig) {
        return BehaviorCatalogEntry.builder()
                .descriptor(BehaviorPlanningMetadata.builder()
                        .key(BehaviorKey.HARVEST_SOUL_SAND)
                        .displayName("Harvest Soul Sand")
                        .description("Gather soul sand from the Nether for potion ingredients")
                        .category(BehaviorCategory.WORK)
                        .intensity(WorkIntensity.HEAVY)
                        .requiredChannel(BehaviorChannel.MOVEMENT)
                        .requiredChannel(BehaviorChannel.INTERACTION)
                        .requiredChannel(BehaviorChannel.COGNITION)
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.harvest_soul_sand")
                        .iconItemId(ResourceLocation.withDefaultNamespace("nether_wart"))
                        .build())
                .factory(() -> new HarvestSoulSandBehavior(config, hungerConfig))
                .build();
    }

    // =========================================================================
    // Farming
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestSugarCane(HarvestSugarCaneConfig config, HungerConfig hungerConfig) {
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
                        .estimatedDuration(ClockTicks.seconds(40).asGameTicks())
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.harvest_sugarcane")
                        .iconItemId(ResourceLocation.withDefaultNamespace("sugar_cane"))
                        .build())
                .factory(() -> new HarvestSugarCaneBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry collectHoney(CollectHoneyConfig config,
                                             HungerConfig hungerConfig,
                                             CollectHoneyYieldDataManager yieldData) {
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.collect_honey")
                        .iconItemId(ResourceLocation.withDefaultNamespace("honey_bottle"))
                        .build())
                .factory(() -> new CollectHoneyBehavior(config, hungerConfig, yieldData))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestHoneycomb(HarvestHoneycombConfig config,
                                                 HungerConfig hungerConfig,
                                                 HarvestHoneycombYieldDataManager yieldData) {
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.harvest_honeycomb")
                        .iconItemId(ResourceLocation.withDefaultNamespace("honeycomb"))
                        .build())
                .factory(() -> new HarvestHoneycombBehavior(config, hungerConfig, yieldData))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry milkCow(MilkCowConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.milk_cow")
                        .iconItemId(ResourceLocation.withDefaultNamespace("milk_bucket"))
                        .build())
                .factory(() -> new MilkCowBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedChickens(BreedAnimalsConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.breed_chickens")
                        .iconItemId(ResourceLocation.withDefaultNamespace("chicken_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.CHICKEN)))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry breedCows(BreedAnimalsConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.breed_cows")
                        .iconItemId(ResourceLocation.withDefaultNamespace("cow_spawn_egg"))
                        .build())
                .factory(() -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.COW)))
                .build();
    }

    // =========================================================================
    // Animal handling
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tameWolf(TameWolfConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.tame_wolf")
                        .iconItemId(ResourceLocation.withDefaultNamespace("bone"))
                        .build())
                .factory(() -> new TameWolfBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry tameCat(TameCatConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.tame_cat")
                        .iconItemId(ResourceLocation.withDefaultNamespace("cod"))
                        .build())
                .factory(() -> new TameCatBehavior(config, hungerConfig))
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.shear_sheep")
                        .iconItemId(ResourceLocation.withDefaultNamespace("shears"))
                        .build())
                .factory(() -> new ShearSheepBehavior(config, hungerConfig, demandSignalService))
                .build();
    }

    // =========================================================================
    // Fishing
    // =========================================================================

    @Provides
    @IntoSet
    static BehaviorCatalogEntry fishing(FishingConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.fishing")
                        .iconItemId(ResourceLocation.withDefaultNamespace("fishing_rod"))
                        .build())
                .factory(() -> new FishingBehavior(config, hungerConfig))
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.cut_stone")
                        .iconItemId(ResourceLocation.withDefaultNamespace("stonecutter"))
                        .build())
                .factory(() -> new CutStoneBehavior(config, hungerConfig))
                .build();
    }

    @Provides
    @IntoSet
    static BehaviorCatalogEntry harvestOre(HarvestOreConfig config, HungerConfig hungerConfig) {
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.harvest_ore")
                        .iconItemId(ResourceLocation.withDefaultNamespace("iron_ore"))
                        .build())
                .factory(() -> new HarvestOreBehavior(config, hungerConfig))
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
                        .interruptible(false)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.enchant_item")
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
                        .interruptible(true)
                        .build())
                .displayInfo(BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.behavior.behavior.walk_dog")
                        .iconItemId(ResourceLocation.withDefaultNamespace("lead"))
                        .build())
                .factory(() -> new WalkDogBehavior(config, hungerConfig))
                .build();
    }

}
