package dev.breezes.settlements.infrastructure.minecraft.behavior.ambient;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import javax.annotation.Nonnull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AmbientBehaviors {

    public static <T extends LivingEntity> BehaviorControl<T> gated(@Nonnull BehaviorControl<? super T> behavior) {
        return new GateBehavior<>(
                ImmutableMap.of(MemoryTypeRegistry.PLAN_BEHAVIOR_ACTIVE.getModuleType(), MemoryStatus.VALUE_ABSENT),
                ImmutableSet.of(),
                GateBehavior.OrderPolicy.ORDERED,
                GateBehavior.RunningPolicy.RUN_ONE,
                ImmutableList.of(Pair.of(behavior, 1)));
    }

}
