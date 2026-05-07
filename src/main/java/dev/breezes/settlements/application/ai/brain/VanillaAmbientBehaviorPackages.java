package dev.breezes.settlements.application.ai.brain;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.infrastructure.minecraft.behavior.ambient.AmbientBehaviors;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.ai.behavior.GoToClosestVillage;
import net.minecraft.world.entity.ai.behavior.InsideBrownianWalk;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.JumpOnBed;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.ShowTradesToPlayer;
import net.minecraft.world.entity.ai.behavior.SleepInBed;
import net.minecraft.world.entity.ai.behavior.SocializeAtBell;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;
import net.minecraft.world.entity.ai.behavior.StrollToPoiList;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;
import net.minecraft.world.entity.ai.behavior.VillageBoundRandomStroll;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.behavior.WorkAtComposter;
import net.minecraft.world.entity.ai.behavior.WorkAtPoi;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public final class VanillaAmbientBehaviorPackages {

    private static final float STROLL_SPEED_MODIFIER = 0.4F;

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getAmbientWorkPackage(
            VillagerProfession profession,
            float speed,
            @Nonnull List<Pair<? extends BehaviorControl<? super Villager>, Integer>> customChoiceBehaviors) {
        BehaviorControl<? super Villager> workAtPoi = profession == VillagerProfession.FARMER
                ? new WorkAtComposter()
                : new WorkAtPoi();

        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> gatedWorkChoiceBehaviors = new ArrayList<>();
        gatedWorkChoiceBehaviors.addAll(List.of(
                Pair.of(workAtPoi, 10),
                Pair.of(StrollAroundPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 4), 2),
                Pair.of(StrollToPoi.create(MemoryModuleType.JOB_SITE, STROLL_SPEED_MODIFIER, 1, 10), 5),
                Pair.of(StrollToPoiList.create(MemoryModuleType.SECONDARY_JOB_SITE, speed, 1, 6, MemoryModuleType.JOB_SITE), 5)
        ));
        gatedWorkChoiceBehaviors.addAll(customChoiceBehaviors);

        return ImmutableList.of(
                VanillaBehaviorPackages.getMinimalLookBehavior(),
                Pair.of(2, AmbientBehaviors.gated(SetWalkTargetFromBlockMemory.create(MemoryModuleType.JOB_SITE, speed, 9, 100, 1200))),
                Pair.of(3, ValidateNearbyPoi.create(profession.heldJobSite(), MemoryModuleType.JOB_SITE)),
                Pair.of(3, AmbientBehaviors.gated(new GiveGiftToHero(100))),
                Pair.of(5, AmbientBehaviors.gated(new RunOne<>(gatedWorkChoiceBehaviors))),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, AmbientBehaviors.gated(SetLookAndInteract.create(EntityType.PLAYER, 4)))
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getAmbientRestPackage(VillagerProfession profession, float speed) {
        return ImmutableList.of(
                VanillaBehaviorPackages.getMinimalLookBehavior(),
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
                        )))
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getAmbientMeetPackage(
            VillagerProfession profession,
            float speed,
            @Nonnull List<Pair<? extends BehaviorControl<? super Villager>, Integer>> customChoiceBehaviors) {
        ArrayList<Pair<? extends BehaviorControl<? super Villager>, Integer>> gatedMeetChoiceBehaviors = new ArrayList<>(customChoiceBehaviors);

        return ImmutableList.of(
                VanillaBehaviorPackages.getFullLookBehavior(),
                Pair.of(1, AmbientBehaviors.gated(new RunOne<>(gatedMeetChoiceBehaviors))),
                Pair.of(2, AmbientBehaviors.gated(TriggerGate.triggerOneShuffled(ImmutableList.of(
                        Pair.of(StrollAroundPoi.create(MemoryModuleType.MEETING_POINT, STROLL_SPEED_MODIFIER, 40), 2),
                        Pair.of(SocializeAtBell.create(), 2)
                )))),
                Pair.of(2, AmbientBehaviors.gated(SetWalkTargetFromBlockMemory.create(MemoryModuleType.MEETING_POINT, speed, 6, 100, 200))),
                Pair.of(3, AmbientBehaviors.gated(new GiveGiftToHero(100))),
                Pair.of(3, ValidateNearbyPoi.create((poiType) -> poiType.is(PoiTypes.MEETING), MemoryModuleType.MEETING_POINT)),
                Pair.of(3, AmbientBehaviors.gated(new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1))))),
                Pair.of(10, new ShowTradesToPlayer(400, 1600)),
                Pair.of(10, AmbientBehaviors.gated(SetLookAndInteract.create(EntityType.PLAYER, 4)))
        );
    }

    public static ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> getAmbientIdlePackage(
            VillagerProfession profession,
            float speed,
            @Nonnull List<Pair<? extends BehaviorControl<? super Villager>, Integer>> customChoiceBehaviors) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> idleBehaviors = new ArrayList<>();

        idleBehaviors.addAll(List.of(
                VanillaBehaviorPackages.getFullLookBehavior(),
                Pair.of(3, AmbientBehaviors.gated(new GiveGiftToHero(100))),
                Pair.of(3, AmbientBehaviors.gated(SetLookAndInteract.create(EntityType.PLAYER, 4))),
                Pair.of(3, new ShowTradesToPlayer(400, 1600)),
                Pair.of(3, AmbientBehaviors.gated(new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new TradeWithVillager(), 1))))),
                Pair.of(3, AmbientBehaviors.gated(new GateBehavior<>(ImmutableMap.of(), ImmutableSet.of(MemoryModuleType.BREED_TARGET), GateBehavior.OrderPolicy.ORDERED,
                        GateBehavior.RunningPolicy.RUN_ONE, ImmutableList.of(Pair.of(new VillagerMakeLove(), 1)))))
        ));

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

        idleChoiceBehaviors.addAll(customChoiceBehaviors);
        idleBehaviors.add(Pair.of(2, AmbientBehaviors.gated(new RunOne<>(idleChoiceBehaviors))));

        return ImmutableList.copyOf(idleBehaviors);
    }

}
