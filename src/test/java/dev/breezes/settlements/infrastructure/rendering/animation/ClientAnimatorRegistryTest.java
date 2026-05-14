package dev.breezes.settlements.infrastructure.rendering.animation;

import dev.breezes.settlements.domain.animation.AnimationArchetype;
import dev.breezes.settlements.domain.animation.AnimationResolver;
import dev.breezes.settlements.domain.animation.KeyframeAnimation;
import dev.breezes.settlements.domain.animation.LoopMode;
import dev.breezes.settlements.domain.animation.VillagerAnimator;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ClientAnimatorRegistryTest {

    @Test
    void getOrCreate_reusesAnimatorForSameVillagerId() {
        // Arrange
        ClientAnimatorRegistry registry = new ClientAnimatorRegistry(resolver());
        int villagerId = 42;
        IntPredicate entityExists = ignored -> true;

        // Act
        VillagerAnimator first = registry.getOrCreate(villagerId, entityExists);
        VillagerAnimator second = registry.getOrCreate(villagerId, entityExists);

        // Assert
        assertSame(first, second);
        assertEquals(1, registry.size());
    }

    @Test
    void getOrCreate_prunesAnimatorStateWhenEntityNoLongerExists() {
        // Arrange
        ClientAnimatorRegistry registry = new ClientAnimatorRegistry(resolver());
        int villagerId = 42;
        Set<Integer> liveEntityIds = ConcurrentHashMap.newKeySet();
        liveEntityIds.add(villagerId);
        IntPredicate entityExists = liveEntityIds::contains;
        VillagerAnimator first = registry.getOrCreate(villagerId, entityExists);

        // Act
        liveEntityIds.remove(villagerId);
        for (int i = 0; i < 599; i++) {
            registry.getOrCreate(1000 + i, entityExists);
        }
        liveEntityIds.add(villagerId);
        VillagerAnimator second = registry.getOrCreate(villagerId, entityExists);

        // Assert
        assertNotSame(first, second);
        assertEquals(2, registry.size());
    }

    private static AnimationResolver resolver() {
        KeyframeAnimation idle = KeyframeAnimation.builder()
                .id(ResourceLocationUtil.mod("animation/test/registry_idle"))
                .durationTicks(0)
                .loopMode(LoopMode.HOLD_LAST)
                .blendInTicks(0)
                .blendOutTicks(0)
                .tracks(List.of())
                .build();
        return (archetype, context) -> {
            if (archetype != AnimationArchetype.IDLE) {
                throw new IllegalArgumentException("Registry test only expects idle initialization");
            }
            return idle;
        };
    }

}
