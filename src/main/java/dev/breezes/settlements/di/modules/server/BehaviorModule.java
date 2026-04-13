package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
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
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.smelting.blastore.BlastOreConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.RepairIronGolemConfig;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsBehavior;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.support.ThrowPotionsConfig;
import dev.breezes.settlements.application.enchanting.engine.EnchantmentEngine;
import dev.breezes.settlements.di.behavior.BehaviorRegistration;
import dev.breezes.settlements.domain.ai.behavior.contracts.IBehavior;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.CollectHoneyYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.data.farming.hive.HarvestHoneycombYieldDataManager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.schedule.Activity;

import java.util.Set;
import java.util.function.Supplier;

@Module
public final class BehaviorModule {

    private static final int CUSTOM_WEIGHT = 10;
    private static final int CUSTOM_PRIORITY = 10;

    /*
     * Armorer
     */
    @Provides
    @IntoSet
    static BehaviorRegistration armorerRepairGolem(RepairIronGolemConfig config) {
        return work(VillagerProfessionKey.ARMORER, () -> new RepairIronGolemBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration armorerBlastOre(BlastOreConfig config) {
        return work(VillagerProfessionKey.ARMORER, () -> new BlastOreBehavior(config));
    }

    /*
     * Butcher
     */

    @Provides
    @IntoSet
    static BehaviorRegistration butcherBreedPigs(BreedAnimalsConfig config) {
        return work(VillagerProfessionKey.BUTCHER, () -> new BreedAnimalsBehavior(config, Set.of(EntityType.PIG)));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration butcherSmokeMeat(SmokeMeatConfig config) {
        return work(VillagerProfessionKey.BUTCHER, () -> new SmokeMeatBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration butcherSlaughter(ButcherLivestockConfig config) {
        return work(VillagerProfessionKey.BUTCHER, () -> new ButcherLivestockBehavior(config));
    }

    /*
     * Cleric
     */
    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsWork(ThrowPotionsConfig config) {
        return work(VillagerProfessionKey.CLERIC, () -> new ThrowPotionsBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsMeet(ThrowPotionsConfig config) {
        return registration(VillagerProfessionKey.CLERIC, Activity.MEET, () -> new ThrowPotionsBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericThrowPotionsIdle(ThrowPotionsConfig config) {
        return registration(VillagerProfessionKey.CLERIC, Activity.IDLE, () -> new ThrowPotionsBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration clericHarvestSoulSand(HarvestSoulSandConfig config) {
        return work(VillagerProfessionKey.CLERIC, () -> new HarvestSoulSandBehavior(config));
    }

    /*
     * Farmer
     */
    @Provides
    @IntoSet
    static BehaviorRegistration farmerHarvestSugarCane(HarvestSugarCaneConfig config) {
        return work(VillagerProfessionKey.FARMER, () -> new HarvestSugarCaneBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerTameWolf(TameWolfConfig config) {
        return work(VillagerProfessionKey.FARMER, () -> new TameWolfBehaviorV2(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerTameCat(TameCatConfig config) {
        return work(VillagerProfessionKey.FARMER, () -> new TameCatBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerBreedChickens(BreedAnimalsConfig config) {
        return work(VillagerProfessionKey.FARMER, () -> new BreedAnimalsBehavior(config, Set.of(EntityType.CHICKEN)));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerMilkCow(MilkCowConfig config) {
        return work(VillagerProfessionKey.FARMER, () -> new MilkCowBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerCollectHoney(CollectHoneyConfig config,
                                                   CollectHoneyYieldDataManager yieldData) {
        return work(VillagerProfessionKey.FARMER, () -> new CollectHoneyBehavior(config, yieldData));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration farmerHarvestHoneycomb(HarvestHoneycombConfig config,
                                                       HarvestHoneycombYieldDataManager yieldData) {
        return work(VillagerProfessionKey.FARMER, () -> new HarvestHoneycombBehavior(config, yieldData));
    }

    /*
     * Fisherman
     */
    @Provides
    @IntoSet
    static BehaviorRegistration fishermanTameCat(TameCatConfig config) {
        return work(VillagerProfessionKey.FISHERMAN, () -> new TameCatBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration fishermanFishing(FishingConfig config) {
        return work(VillagerProfessionKey.FISHERMAN, () -> new FishingBehavior(config));
    }

    /*
     * Fletcher
     */
    @Provides
    @IntoSet
    static BehaviorRegistration fletcherBreedChickens(BreedAnimalsConfig config) {
        return work(VillagerProfessionKey.FLETCHER, () -> new BreedAnimalsBehavior(config, Set.of(EntityType.CHICKEN)));
    }

    /*
     * Leatherworker
     */
    @Provides
    @IntoSet
    static BehaviorRegistration leatherworkerBreedCows(BreedAnimalsConfig config) {
        return work(VillagerProfessionKey.LEATHERWORKER, () -> new BreedAnimalsBehavior(config, Set.of(EntityType.COW)));
    }

    /*
     * Librarian
     */
    @Provides
    @IntoSet
    static BehaviorRegistration librarianEnchant(EnchantItemConfig config, EnchantmentEngine enchantmentEngine) {
        return work(VillagerProfessionKey.LIBRARIAN, () -> new EnchantItemBehavior(config, enchantmentEngine));
    }

    /*
     * Mason
     */
    @Provides
    @IntoSet
    static BehaviorRegistration masonCutStone(CutStoneConfig config) {
        return work(VillagerProfessionKey.MASON, () -> new CutStoneBehavior(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration masonHarvestOre(HarvestOreConfig config) {
        return work(VillagerProfessionKey.MASON, () -> new HarvestOreBehavior(config));
    }

    /*
     * Shepherd
     */
    @Provides
    @IntoSet
    static BehaviorRegistration shepherdShearSheep(ShearSheepConfig config) {
        return work(VillagerProfessionKey.SHEPHERD, () -> new ShearSheepBehaviorV2(config));
    }

    @Provides
    @IntoSet
    static BehaviorRegistration shepherdTameWolf(TameWolfConfig config) {
        return work(VillagerProfessionKey.SHEPHERD, () -> new TameWolfBehaviorV2(config));
    }

    /*
     * Toolsmith
     */
    @Provides
    @IntoSet
    static BehaviorRegistration toolsmithRepairGolem(RepairIronGolemConfig config) {
        return work(VillagerProfessionKey.TOOLSMITH, () -> new RepairIronGolemBehavior(config));
    }

    /*
     * Weaponsmith
     */
    @Provides
    @IntoSet
    static BehaviorRegistration weaponsmithRepairGolem(RepairIronGolemConfig config) {
        return work(VillagerProfessionKey.WEAPONSMITH, () -> new RepairIronGolemBehavior(config));
    }

    // NOTE: To register a new behavior: add a @Provides @IntoSet method here.
    // Adding a new custom profession: add the constant to VillagerProfessionKey first,
    // then use it here. Compile fails if a referenced config has no Dagger binding.

    private static BehaviorRegistration work(VillagerProfessionKey villagerProfessionKey, Supplier<IBehavior<BaseVillager>> behaviorFactory) {
        return registration(villagerProfessionKey, Activity.WORK, behaviorFactory);
    }

    private static BehaviorRegistration registration(
            VillagerProfessionKey villagerProfessionKey,
            Activity activity,
            Supplier<IBehavior<BaseVillager>> behaviorFactory) {
        return BehaviorRegistration.builder()
                .profession(villagerProfessionKey)
                .behaviorFactory(behaviorFactory)
                .activity(activity)
                .weight(CUSTOM_WEIGHT)
                .priority(CUSTOM_PRIORITY)
                .build();
    }

}
