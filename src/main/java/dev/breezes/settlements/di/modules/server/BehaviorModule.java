package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.BreedAnimalsConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepBehaviorV2;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.ShearSheepConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameCatConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.animals.TameWolfBehaviorV2;
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
import dev.breezes.settlements.di.behavior.BehaviorRegistration;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.economy.catalog.TradeCatalogRegistry;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Module
public final class BehaviorModule {

    private static final int CUSTOM_WEIGHT = 10;
    private static final int CUSTOM_PRIORITY = 10;

    private static final int TRADE_INITIATE_WEIGHT = 10;
    private static final int TRADE_INITIATE_PRIORITY = 10;
    private static final int TRADE_ACCEPT_WEIGHT = 15;
    private static final int TRADE_ACCEPT_PRIORITY = 15;

    private static final List<VillagerProfessionKey> TRADE_PROFESSIONS = List.of(
            VillagerProfessionKey.NONE,
            VillagerProfessionKey.ARMORER,
            VillagerProfessionKey.BUTCHER,
            VillagerProfessionKey.CARTOGRAPHER,
            VillagerProfessionKey.CLERIC,
            VillagerProfessionKey.FARMER,
            VillagerProfessionKey.FISHERMAN,
            VillagerProfessionKey.FLETCHER,
            VillagerProfessionKey.LEATHERWORKER,
            VillagerProfessionKey.LIBRARIAN,
            VillagerProfessionKey.MASON,
            VillagerProfessionKey.NITWIT,
            VillagerProfessionKey.SHEPHERD,
            VillagerProfessionKey.TOOLSMITH,
            VillagerProfessionKey.WEAPONSMITH);

    /*
     * Armorer
     */
    @Provides
    @IntoSet
    static BehaviorRegistration armorerRepairGolem(RepairIronGolemConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.ARMORER, () -> new RepairIronGolemBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration armorerBlastOre(BlastOreConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.ARMORER, () -> new BlastOreBehavior(config, hungerConfig));
    }

    /*
     * Butcher
     */

    @Provides
    @IntoSet
    static BehaviorRegistration butcherBreedPigs(BreedAnimalsConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.BUTCHER, () -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.PIG)));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration butcherSmokeMeat(SmokeMeatConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.BUTCHER, () -> new SmokeMeatBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration butcherSlaughter(ButcherLivestockConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.BUTCHER, () -> new ButcherLivestockBehavior(config, hungerConfig));
    }

    /*
     * Cleric
     */
    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsWork(ThrowPotionsConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.CLERIC, () -> new ThrowPotionsBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsMeet(ThrowPotionsConfig config, HungerConfig hungerConfig) {
        return registration(VillagerProfessionKey.CLERIC, Activity.MEET, () -> new ThrowPotionsBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsIdle(ThrowPotionsConfig config, HungerConfig hungerConfig) {
        return registration(VillagerProfessionKey.CLERIC, Activity.IDLE, () -> new ThrowPotionsBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericHarvestSoulSand(HarvestSoulSandConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.CLERIC, () -> new HarvestSoulSandBehavior(config, hungerConfig));
    }

    /*
     * Farmer
     */
    @Provides
    @IntoSet
    static BehaviorRegistration farmerHarvestSugarCane(HarvestSugarCaneConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FARMER, () -> new HarvestSugarCaneBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerTameWolf(TameWolfConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FARMER, () -> new TameWolfBehaviorV2(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerTameCat(TameCatConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FARMER, () -> new TameCatBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerBreedChickens(BreedAnimalsConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FARMER, () -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.CHICKEN)));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerMilkCow(MilkCowConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FARMER, () -> new MilkCowBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerCollectHoney(CollectHoneyConfig config,
                                                   HungerConfig hungerConfig,
                                                   CollectHoneyYieldDataManager yieldData) {
        return work(VillagerProfessionKey.FARMER, () -> new CollectHoneyBehavior(config, hungerConfig, yieldData));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerHarvestHoneycomb(HarvestHoneycombConfig config,
                                                       HungerConfig hungerConfig,
                                                       HarvestHoneycombYieldDataManager yieldData) {
        return work(VillagerProfessionKey.FARMER, () -> new HarvestHoneycombBehavior(config, hungerConfig, yieldData));
    }

    /*
     * Fisherman
     */
    @Provides
    @IntoSet
    static BehaviorRegistration fishermanTameCat(TameCatConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FISHERMAN, () -> new TameCatBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration fishermanFishing(FishingConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FISHERMAN, () -> new FishingBehavior(config, hungerConfig));
    }

    /*
     * Fletcher
     */
    @Provides
    @IntoSet
    static BehaviorRegistration fletcherBreedChickens(BreedAnimalsConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.FLETCHER, () -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.CHICKEN)));
    }

    /*
     * Leatherworker
     */
    @Provides
    @IntoSet
    static BehaviorRegistration leatherworkerBreedCows(BreedAnimalsConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.LEATHERWORKER, () -> new BreedAnimalsBehavior(config, hungerConfig, Set.of(EntityType.COW)));
    }

    /*
     * Librarian
     */
    @Provides
    @IntoSet
    static BehaviorRegistration librarianEnchant(EnchantItemConfig config,
                                                 HungerConfig hungerConfig,
                                                 EnchantmentEngine enchantmentEngine) {
        return work(VillagerProfessionKey.LIBRARIAN, () -> new EnchantItemBehavior(config, hungerConfig, enchantmentEngine));
    }

    /*
     * Mason
     */
    @Provides
    @IntoSet
    static BehaviorRegistration masonCutStone(CutStoneConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.MASON, () -> new CutStoneBehavior(config, hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration masonHarvestOre(HarvestOreConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.MASON, () -> new HarvestOreBehavior(config, hungerConfig));
    }

    /*
     * Shepherd
     */
    @Provides
    @IntoSet
    static BehaviorRegistration shepherdShearSheep(ShearSheepConfig config,
                                                   HungerConfig hungerConfig,
                                                   DemandSignalService demandSignalService) {
        return work(VillagerProfessionKey.SHEPHERD, () -> new ShearSheepBehaviorV2(config, hungerConfig, demandSignalService));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration shepherdTameWolf(TameWolfConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.SHEPHERD, () -> new TameWolfBehaviorV2(config, hungerConfig));
    }

    /*
     * Toolsmith
     */
    @Provides
    @IntoSet
    static BehaviorRegistration toolsmithRepairGolem(RepairIronGolemConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.TOOLSMITH, () -> new RepairIronGolemBehavior(config, hungerConfig));
    }

    /*
     * Weaponsmith
     */
    @Provides
    @IntoSet
    static BehaviorRegistration weaponsmithRepairGolem(RepairIronGolemConfig config, HungerConfig hungerConfig) {
        return work(VillagerProfessionKey.WEAPONSMITH, () -> new RepairIronGolemBehavior(config, hungerConfig));
    }

    @Provides
    @ElementsIntoSet
    static Set<BehaviorRegistration> tradeMeetBehaviors(TradingConfig config,
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
        return TRADE_PROFESSIONS.stream()
                .flatMap(profession -> Stream.of(
                        tradeAccept(profession, config, hungerConfig, sessionRegistry, tradeSessionPresenter),
                        tradeInitiate(profession, config, hungerConfig, sessionRegistry, tradeCatalogRegistry, tradePriceResolver,
                                demandSignalService, villagerWallet, tradeExecutor, tradeSessionPresenter, demandEvaluator,
                                partnerScanner, negotiationEngine)))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Provides
    @IntoSet
    static BehaviorRegistration noneEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.NONE, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration armorerEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.ARMORER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration butcherEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.BUTCHER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration cartographerEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.CARTOGRAPHER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.CLERIC, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.FARMER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration fishermanEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.FISHERMAN, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration fletcherEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.FLETCHER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration leatherworkerEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.LEATHERWORKER, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration librarianEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.LIBRARIAN, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration masonEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.MASON, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration nitwitEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.NITWIT, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration shepherdEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.SHEPHERD, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration toolsmithEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.TOOLSMITH, () -> new EatFoodBehavior(hungerConfig));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration weaponsmithEat(HungerConfig hungerConfig) {
        return core(VillagerProfessionKey.WEAPONSMITH, () -> new EatFoodBehavior(hungerConfig));
    }

    // NOTE: To register a new behavior: add a @Provides @IntoSet method here.
    // Adding a new custom profession: add the constant to VillagerProfessionKey first,
    // then use it here. Compile fails if a referenced config has no Dagger binding.

    private static BehaviorRegistration work(VillagerProfessionKey villagerProfessionKey, Supplier<IBehavior<BaseVillager>> behaviorFactory) {
        return registration(villagerProfessionKey, Activity.WORK, behaviorFactory);
    }

    private static BehaviorRegistration core(VillagerProfessionKey villagerProfessionKey,
                                             Supplier<IBehavior<BaseVillager>> behaviorFactory) {
        return registration(villagerProfessionKey, Activity.CORE, behaviorFactory);
    }

    private static BehaviorRegistration registration(
            VillagerProfessionKey villagerProfessionKey,
            Activity activity,
            Supplier<IBehavior<BaseVillager>> behaviorFactory) {
        return registration(villagerProfessionKey, activity, behaviorFactory, CUSTOM_WEIGHT, CUSTOM_PRIORITY);
    }

    private static BehaviorRegistration registration(
            VillagerProfessionKey villagerProfessionKey,
            Activity activity,
            Supplier<IBehavior<BaseVillager>> behaviorFactory,
            int weight,
            int priority) {
        return BehaviorRegistration.builder()
                .profession(villagerProfessionKey)
                .behaviorFactory(behaviorFactory)
                .activity(activity)
                .weight(weight)
                .priority(priority)
                .build();
    }

    private static BehaviorRegistration meet(VillagerProfessionKey villagerProfessionKey,
                                             Supplier<IBehavior<BaseVillager>> behaviorFactory,
                                             int weight,
                                             int priority) {
        return registration(villagerProfessionKey, Activity.MEET, behaviorFactory, weight, priority);
    }

    private static BehaviorRegistration tradeAccept(VillagerProfessionKey profession,
                                                    TradingConfig config,
                                                    HungerConfig hungerConfig,
                                                    TradeSessionRegistry sessionRegistry,
                                                    TradeSessionPresenter tradeSessionPresenter) {
        return meet(profession,
                () -> new TradeAcceptBehavior(config, hungerConfig, sessionRegistry, tradeSessionPresenter),
                TRADE_ACCEPT_WEIGHT,
                TRADE_ACCEPT_PRIORITY);
    }

    private static BehaviorRegistration tradeInitiate(VillagerProfessionKey profession,
                                                      TradingConfig config,
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
        return meet(profession,
                () -> new TradeInitiateBehavior(config, hungerConfig, sessionRegistry, tradeCatalogRegistry, tradePriceResolver,
                        demandSignalService, villagerWallet, tradeExecutor, tradeSessionPresenter, demandEvaluator, partnerScanner,
                        negotiationEngine),
                TRADE_INITIATE_WEIGHT,
                TRADE_INITIATE_PRIORITY);
    }

}
