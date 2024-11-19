package dev.breezes.settlements.models.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.entities.villager.BaseVillager;
import dev.breezes.settlements.models.behaviors.BaseVillagerBehavior;
import dev.breezes.settlements.models.behaviors.BlastOreBehavior;
import dev.breezes.settlements.models.behaviors.BreedAnimalsBehavior;
import dev.breezes.settlements.models.behaviors.CutStoneBehavior;
import dev.breezes.settlements.models.behaviors.DefaultBehaviorAdapter;
import dev.breezes.settlements.models.behaviors.IBehavior;
import dev.breezes.settlements.models.behaviors.RepairIronGolemBehavior;
import dev.breezes.settlements.models.behaviors.ShearSheepBehavior;
import dev.breezes.settlements.models.behaviors.ThrowPotionsBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.behavior.AcquirePoi;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.CelebrateVillagersSurvivedRaid;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.GoToClosestVillage;
import net.minecraft.world.entity.ai.behavior.GoToPotentialJobSite;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.HarvestFarmland;
import net.minecraft.world.entity.ai.behavior.InsideBrownianWalk;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.JumpOnBed;
import net.minecraft.world.entity.ai.behavior.LocateHidingPlace;
import net.minecraft.world.entity.ai.behavior.LookAndFollowTradingPlayerSink;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MoveToSkySeeingSpot;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.PlayTagWithOtherKids;
import net.minecraft.world.entity.ai.behavior.PoiCompetitorScan;
import net.minecraft.world.entity.ai.behavior.ReactToBell;
import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.ai.behavior.ResetRaidStatus;
import net.minecraft.world.entity.ai.behavior.RingBell;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetHiddenState;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetRaidStatus;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.behavior.SocializeAtBell;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.ai.behavior.VillagerCalmDown;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.behavior.VillagerPanicTrigger;
import net.minecraft.world.entity.ai.behavior.WakeUp;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.behavior.YieldJobSite;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.raid.Raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Mostly copied from VillagerGoalPackages class
 * - custom behaviors are added
 */
public final class CustomBehaviorPackages {

    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    /**
     * Core behaviors
     */
    public static BehaviorContainer getCorePackage(VillagerProfession profession, float speed) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> coreBehaviors = new ArrayList<>();

        // Add default behaviors
        coreBehaviors.addAll(List.of(
                Pair.of(0, new Swim(0.8F)),
                Pair.of(0, InteractWithDoor.create()),
                Pair.of(0, new LookAtTargetSink(45, 90)),
                Pair.of(0, new VillagerPanicTrigger()),
                Pair.of(0, WakeUp.create()),
                Pair.of(0, ReactToBell.create()),
                Pair.of(0, SetRaidStatus.create()),
                Pair.of(0, ValidateNearbyPoi.create(profession.heldJobSite(), MemoryModuleType.JOB_SITE)),
                Pair.of(0, ValidateNearbyPoi.create(profession.acquirableJobSite(), MemoryModuleType.POTENTIAL_JOB_SITE)),
                Pair.of(1, new MoveToTargetSink()),
                Pair.of(2, PoiCompetitorScan.create()),
                Pair.of(3, new LookAndFollowTradingPlayerSink(speed)),
                Pair.of(5, GoToWantedItem.create(speed, false, 4)),
                Pair.of(6, AcquirePoi.create(profession.acquirableJobSite(), MemoryModuleType.JOB_SITE, MemoryModuleType.POTENTIAL_JOB_SITE, true,
                        Optional.empty())),
                Pair.of(7, new GoToPotentialJobSite(speed)),
                Pair.of(8, YieldJobSite.create(speed)),
                Pair.of(10, AcquirePoi.create((poiType) -> poiType.is(PoiTypes.HOME), MemoryModuleType.HOME, false, Optional.of((byte) 14))),
                Pair.of(10, AcquirePoi.create((poiType) -> poiType.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT, true, Optional.of((byte) 14))),
                Pair.of(10, AssignProfessionFromJobSite.create()),
                Pair.of(10, ResetProfession.create())
        ));

        // Add custom behaviors
        List<IBehavior<BaseVillager>> customBehaviors = new ArrayList<>();

        // Eat meals behavior
//        addBehavior(new EatAtMealTimeBehavior(), coreBehaviors, 4, customBehaviors);

        // Drink water behavior
//        addBehavior(new DrinkWaterBehavior(), coreBehaviors, 4, customBehaviors);

        // Scan for pets behavior (not added to GUI)
//        coreBehaviors.add(Pair.of(20, ScanForPetsBehaviorController.scanForPets()));

        // Return behaviors
        return new BehaviorContainer(ImmutableList.copyOf(coreBehaviors), customBehaviors);
    }

    /**
     * Work activity behaviors
     */
    public static BehaviorContainer getWorkPackage(VillagerProfession profession, float speed) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> workChoiceBehaviors = new ArrayList<>();

        // Add default behaviors
        workChoiceBehaviors.addAll(List.of(
                Pair.of(profession == VillagerProfession.FARMER ? new WorkAtComposter() : new WorkAtPoi(), 7),
                Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 4), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 1, 10), 5),
                Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5),
                Pair.of(new HarvestFarmland(), profession == VillagerProfession.FARMER ? 2 : 5),
                Pair.of(new UseBonemeal(), profession == VillagerProfession.FARMER ? 4 : 7)
        ));

        /*
         * Assign custom work behaviors based on profession
         */

        // Map of { behavior => weight of behavior }
        Map<Behavior<Villager>, Integer> customBehaviorWeightMap = new HashMap<>();
        int customGoalWeight = 10;

        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            // Unreachable code, because villager does not have job site
        } else if (profession == VillagerProfession.ARMORER) {
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new RepairIronGolemBehavior()), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BlastOreBehavior()), customGoalWeight);
        } else if (profession == VillagerProfession.BUTCHER) {
//            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BreedAnimalsBehavior(Set.of(EntityType.PIG))), customGoalWeight);
//            customBehaviorWeightMap.put(new ButcherAnimalsBehavior(Map.of(
//                    EntityType.COW, 3,
//                    EntityType.SHEEP, 5,
//                    EntityType.CHICKEN, 3,
//                    EntityType.PIG, 2,
//                    EntityType.RABBIT, 2
//            )), customGoalWeight);
        } else if (profession == VillagerProfession.CARTOGRAPHER) {
            // TODO: add behavior
        } else if (profession == VillagerProfession.CLERIC) {
            // TODO: add behavior
        } else if (profession == VillagerProfession.FARMER) {
//            customBehaviorWeightMap.put(new HarvestSugarcaneBehavior(), customGoalWeight);
//            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
//            customBehaviorWeightMap.put(new TameCatBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN))), customGoalWeight);
//            customBehaviorWeightMap.put(new MakeCakeBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.FISHERMAN) {
//            customBehaviorWeightMap.put(new TameCatBehavior(), customGoalWeight);
//            customBehaviorWeightMap.put(new FishingBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.FLETCHER) {
//            customBehaviorWeightMap.put(new CollectArrowsBehavior(), customGoalWeight);
//            customBehaviorWeightMap.put(new MakeTippedArrowsBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BreedAnimalsBehavior(Set.of(EntityType.CHICKEN))), customGoalWeight);
        } else if (profession == VillagerProfession.LEATHERWORKER) {
//            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BreedAnimalsBehavior(Set.of(EntityType.COW))), customGoalWeight);
        } else if (profession == VillagerProfession.LIBRARIAN) {
//            customBehaviorWeightMap.put(new EnchantItemBehavior(), customGoalWeight);
        } else if (profession == VillagerProfession.MASON) {
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new CutStoneBehavior()), customGoalWeight);
        } else if (profession == VillagerProfession.SHEPHERD) {
//            customBehaviorWeightMap.put(new TameWolfBehavior(), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new ShearSheepBehavior()), customGoalWeight);
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new BreedAnimalsBehavior(Set.of(EntityType.SHEEP))), customGoalWeight);
        } else if (profession == VillagerProfession.TOOLSMITH) {
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new RepairIronGolemBehavior()), customGoalWeight);
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            customBehaviorWeightMap.put(DefaultBehaviorAdapter.adapt(new RepairIronGolemBehavior()), customGoalWeight);
        }

        // Add custom behaviors
        for (Map.Entry<Behavior<Villager>, Integer> entry : customBehaviorWeightMap.entrySet()) {
            addChoiceBehavior(entry.getKey(), workChoiceBehaviors, entry.getValue());
        }

        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(5, new RunOne<>(workChoiceBehaviors)),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );

        return new BehaviorContainer(behaviors, new ArrayList<>());
    }

    /**
     * Play activity behaviors
     * - only used in baby villagers
     */
    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPlayPackage(float speed) {
        return ImmutableList.of(
                getFullLookBehavior(),
                Pair.of(0, new MoveToTargetSink(80, 120)),
                Pair.of(5, PlayTagWithOtherKids.create()),
                Pair.of(5, new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.VISIBLE_VILLAGER_BABIES, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                                Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1),
                                Pair.of(VillageBoundRandomStroll.create(speed), 1),
                                Pair.of(SetWalkTargetFromLookTarget.create(speed, 2), 1),
                                Pair.of(new JumpOnBed(speed), 2),
                                Pair.of(new DoNothing(20, 40), 2)
                        ))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRestPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.HOME, speed, 1, 150, 1200)),
                Pair.of(3, ValidateNearbyPoi.create((poiType) -> poiType.is(PoiTypes.HOME), MemoryModuleType.HOME)),
                Pair.of(3, new SleepInBed()),
                Pair.of(5, new RunOne<>(
                        ImmutableMap.of(MemoryModuleType.HOME, MemoryStatus.VALUE_ABSENT),
                        ImmutableList.of(
                                Pair.of(SetClosestHomeAsWalkTarget.create(speed), 1),
                                Pair.of(InsideBrownianWalk.create(speed), 4),
                                Pair.of(GoToClosestVillage.create(speed, 4), 2),
                                Pair.of(new DoNothing(20, 40), 2)
                        ))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );
    }

    public static BehaviorContainer getMeetPackage(VillagerProfession profession, float speed) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> customMeetChoiceBehaviors = new ArrayList<>();

        // Internal trade behavior (higher weight)
//        addChoiceBehavior(new TradeItemsBehavior(), customMeetChoiceBehaviors, 5, customBehaviors);

        // Feed wolf behavior
        if (profession == VillagerProfession.BUTCHER) {
//            addChoiceBehavior(new FeedWolfBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Tame wolf behavior
        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
                || profession == VillagerProfession.BUTCHER) {
//            addChoiceBehavior(new TameWolfBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Tame cat behavior
        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
//            addChoiceBehavior(new TameCatBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }

        // Throw healing potion behavior
        if (profession == VillagerProfession.CLERIC) {
            addChoiceBehavior(DefaultBehaviorAdapter.adapt(new ThrowPotionsBehavior()), customMeetChoiceBehaviors, 10);
        }

        // Nitwit behaviors
        if (profession == VillagerProfession.NITWIT) {
//            addChoiceBehavior(new RingBellBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
//            addChoiceBehavior(new LaunchFireworkBehavior(), customMeetChoiceBehaviors, 1, customBehaviors);
        }


        // Default behaviors
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors = ImmutableList.of(
                getFullLookBehavior(),
                Pair.of(1, new RunOne<>(customMeetChoiceBehaviors)),
                Pair.of(2, TriggerGate.triggerOneShuffled(ImmutableList.of(
                        Pair.of(StrollAroundPoi.create(MemoryModuleType.MEETING_POINT, STROLL_SPEED_MODIFIER, 40), 2),
                        Pair.of(SocializeAtBell.create(), 2)
                ))),
                Pair.of(2, SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200)),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, ValidateNearbyPoi.create((poiType) -> poiType.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT)),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1)))),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(99, UpdateActivityFromSchedule.create())
        );

        return new BehaviorContainer(behaviors, new ArrayList<>());
    }

    public static BehaviorContainer getIdlePackage(VillagerProfession profession, float speed) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> idleBehaviors = new ArrayList<>();
        List<BaseVillagerBehavior> customBehaviors = new ArrayList<>();

        idleBehaviors.addAll(List.of(
                getFullLookBehavior(),
                Pair.of(3, new GiveGiftToHero(100)),
                Pair.of(3, SetLookAndInteract.create(EntityType.PLAYER, 4)),
                Pair.of(3, new ShowTradesToPlayer(400, 1600)),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1)))),
                Pair.of(3, new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new VillagerMakeLove(), 1)))),
                Pair.of(99, UpdateActivityFromSchedule.create())
        ));

        // Default behaviors that will be randomly chosen to run one
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> idleChoiceBehaviors = new ArrayList<>();
        idleChoiceBehaviors.addAll(List.of(
                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 2),
                Pair.of(InteractWith.of(EntityType.VILLAGER, 8, AgeableMob::canBreed, AgeableMob::canBreed, MemoryModuleType.BREED_TARGET, speed, 2), 1),
                Pair.of(InteractWith.of(EntityType.CAT, 8, MemoryModuleType.INTERACTION_TARGET, speed, 2), 1),
                Pair.of(VillageBoundRandomStroll.create(speed), 1),
                Pair.of(SetWalkTargetFromLookTarget.create(speed, 2), 1),
                Pair.of(new JumpOnBed(speed), 1),
                Pair.of(new DoNothing(30, 60), 1)
        ));

        /*
         * Add custom behaviors
         */
        // Custom wolf-related behaviors
//        if (profession == VillagerProfession.SHEPHERD || profession == VillagerProfession.FARMER || profession == VillagerProfession.LEATHERWORKER
//                || profession == VillagerProfession.BUTCHER) {
//            // Add parallel-running behaviors
//            addBehavior(new WalkDogBehavior(), idleBehaviors, 1, customBehaviors);
//
//            // Add choice behaviors
//            for (BaseVillagerBehavior behavior : List.of(
//                    new TameWolfBehavior(),
//                    new WashWolfBehavior()
//            )) {
//                addChoiceBehavior(behavior, idleChoiceBehaviors, 10, customBehaviors);
//            }
//        }

        // Tame cat behavior (cat should be resting now, so no other behaviors)
//        if (profession == VillagerProfession.FARMER || profession == VillagerProfession.FISHERMAN) {
//            addChoiceBehavior(new TameCatBehavior(), idleChoiceBehaviors, 10, customBehaviors);
//        }

        // Nitwit behaviors
//        if (profession == VillagerProfession.NITWIT) {
//            // Add parallel-running behaviors
//            addBehavior(new LaunchFireworkBehavior(), idleBehaviors, 1, customBehaviors);
//            addBehavior(new ThrowSnowballBehavior(), idleBehaviors, 1, customBehaviors);
//            addBehavior(new RunAroundBehavior(), idleBehaviors, 1, customBehaviors);
//        }

        if (profession == VillagerProfession.CLERIC) {
            addChoiceBehavior(DefaultBehaviorAdapter.adapt(new ThrowPotionsBehavior()), idleChoiceBehaviors, 10);
        }

        // Add choice behaviors to Minecraft behaviors
        idleBehaviors.add(Pair.of(2, new RunOne<>(idleChoiceBehaviors)));

        return new BehaviorContainer(ImmutableList.copyOf(idleBehaviors), new ArrayList<>());
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPanicPackage(VillagerProfession profession, float speed) {
        float panicSpeed = speed * 1.5F;
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, VillagerCalmDown.create()),
                Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.NEAREST_HOSTILE, panicSpeed, 6, false)),
                Pair.of(1, SetWalkTargetAwayFrom.entity(MemoryModuleType.HURT_BY_ENTITY, panicSpeed, 6, false)),
                Pair.of(3, VillageBoundRandomStroll.create(panicSpeed, 2, 2))
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getPreRaidPackage(VillagerProfession profession, float speed) {
        float panicSpeed = speed * 1.5F;
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, RingBell.create()),
                Pair.of(0, TriggerGate.triggerOneShuffled(ImmutableList.of(
                        Pair.of(SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, panicSpeed, 2, 150, 200), 6),
                        Pair.of(VillageBoundRandomStroll.create(panicSpeed), 2)
                ))),
                Pair.of(99, ResetRaidStatus.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getRaidPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, BehaviorBuilder.sequence(
                        BehaviorBuilder.triggerIf(CustomBehaviorPackages::raidWon),
                        TriggerGate.triggerOneShuffled(ImmutableList.of(
                                Pair.of(MoveToSkySeeingSpot.create(speed), 5),
                                Pair.of(VillageBoundRandomStroll.create(speed * 1.1F), 2)
                        ))
                )),
                Pair.of(0, new CelebrateVillagersSurvivedRaid(600, 600)),
                Pair.of(2, BehaviorBuilder.sequence(
                        BehaviorBuilder.triggerIf(CustomBehaviorPackages::hasActiveRaid),
                        LocateHidingPlace.create(24, speed * 1.4F, 1)
                )),
                Pair.of(99, ResetRaidStatus.create())
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getHidePackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                getMinimalLookBehavior(),
                Pair.of(0, SetHiddenState.create(15, 3)),
                Pair.of(1, LocateHidingPlace.create(32, speed * 1.25F, 2))
        );
    }

    /**
     * Usually used when the villager is "busy"
     */
    private static Pair<Integer, BehaviorControl<LivingEntity>> getMinimalLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(new DoNothing(30, 60), 8)
        )));
    }

    /**
     * Usually used when the villager is "free"
     */
    private static Pair<Integer, BehaviorControl<LivingEntity>> getFullLookBehavior() {
        return Pair.of(5, new RunOne<>(ImmutableList.of(
                Pair.of(SetEntityLookTarget.create(EntityType.CAT, 8.0F), 8),
                Pair.of(SetEntityLookTarget.create(EntityType.VILLAGER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 2),
                Pair.of(SetEntityLookTarget.create(MobCategory.CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.WATER_CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.AXOLOTLS, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.UNDERGROUND_WATER_CREATURE, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.WATER_AMBIENT, 8.0F), 1),
                Pair.of(SetEntityLookTarget.create(MobCategory.MONSTER, 8.0F), 1),
                Pair.of(new DoNothing(30, 60), 2)
        )));
    }

    private static boolean hasActiveRaid(ServerLevel world, LivingEntity entity) {
        Raid raid = world.getRaidAt(entity.blockPosition());
        return raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
    }

    private static boolean raidWon(ServerLevel world, LivingEntity entity) {
        Raid raid = world.getRaidAt(entity.blockPosition());
        return raid != null && raid.isVictory();
    }

    /*
     * Utility methods
     */
    private static void addBehavior(Behavior<Villager> behavior,
                                    List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> minecraftBehaviors, int weight) {
        minecraftBehaviors.add(Pair.of(weight, behavior));
    }

    private static void addChoiceBehavior(Behavior<Villager> behavior,
                                          List<Pair<? extends BehaviorControl<? super Villager>, Integer>> choiceBehaviors, int weight) {
        choiceBehaviors.add(Pair.of(behavior, weight));
    }

    /**
     * Record used as return data structure
     *
     * @param behaviors       minecraft behaviors to be registered
     * @param customBehaviors custom behaviors (must be same instance)
     */
    public record BehaviorContainer(ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> behaviors,
                                    List<IBehavior<BaseVillager>> customBehaviors) {
    }

}
