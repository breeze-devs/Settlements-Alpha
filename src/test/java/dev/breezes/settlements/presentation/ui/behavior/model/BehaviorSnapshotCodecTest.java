package dev.breezes.settlements.presentation.ui.behavior.model;

import dev.breezes.settlements.application.ui.behavior.model.BehaviorControllerSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.BehaviorRowSnapshot;
import dev.breezes.settlements.application.ui.behavior.model.PreconditionSummary;
import dev.breezes.settlements.application.ui.behavior.model.SchedulePhase;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.codec.BehaviorControllerSnapshotCodec;
import dev.breezes.settlements.infrastructure.network.features.ui.behavior.codec.BehaviorRowSnapshotCodec;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class BehaviorSnapshotCodecTest {

    @Test
    void behaviorRowSnapshot_roundtrip_preservesFields() {
        BehaviorRowSnapshot input = BehaviorRowSnapshot.builder()
                .behaviorId("shear_sheep")
                .displayNameKey("ui.settlements.behavior.mock.behavior.shear_sheep")
                .displaySuffix("(sheep)")
                .iconItemId(ResourceLocation.withDefaultNamespace("shears"))
                .priority(80)
                .uiBehaviorIndex(2)
                .registeredSchedules(List.of(SchedulePhase.WORK, SchedulePhase.MEET))
                .running(true)
                .currentStageLabel("Shear")
                .cooldownRemainingTicks(40)
                .preconditionSummary(PreconditionSummary.PASS)
                .build();

        FriendlyByteBuf buffer = createBuffer();
        BehaviorRowSnapshotCodec.write(buffer, input);

        BehaviorRowSnapshot decoded = BehaviorRowSnapshotCodec.read(buffer);
        Assertions.assertEquals(input, decoded);
    }

    @Test
    void behaviorControllerSnapshot_roundtrip_preservesFields() {
        BehaviorRowSnapshot rowA = BehaviorRowSnapshot.builder()
                .behaviorId("harvest_sugar_cane")
                .displayNameKey("ui.settlements.behavior.mock.behavior.harvest_sugar_cane")
                .displaySuffix(null)
                .iconItemId(ResourceLocation.withDefaultNamespace("sugar_cane"))
                .priority(90)
                .uiBehaviorIndex(3)
                .registeredSchedules(List.of(SchedulePhase.WORK))
                .running(false)
                .currentStageLabel(null)
                .cooldownRemainingTicks(30)
                .preconditionSummary(PreconditionSummary.FAIL)
                .build();
        BehaviorRowSnapshot rowB = BehaviorRowSnapshot.builder()
                .behaviorId("shear_sheep")
                .displayNameKey("ui.settlements.behavior.mock.behavior.shear_sheep")
                .displaySuffix("(sheep)")
                .iconItemId(ResourceLocation.withDefaultNamespace("shears"))
                .priority(80)
                .uiBehaviorIndex(2)
                .registeredSchedules(List.of(SchedulePhase.WORK, SchedulePhase.MEET))
                .running(true)
                .currentStageLabel("Shear")
                .cooldownRemainingTicks(0)
                .preconditionSummary(PreconditionSummary.PASS)
                .build();

        BehaviorControllerSnapshot input = new BehaviorControllerSnapshot(
                12345L,
                17,
                "Debug Villager",
                SchedulePhase.WORK,
                "minecraft:work",
                List.of(rowA, rowB)
        );

        FriendlyByteBuf buffer = createBuffer();
        BehaviorControllerSnapshotCodec.write(buffer, input);

        BehaviorControllerSnapshot decoded = BehaviorControllerSnapshotCodec.read(buffer);
        Assertions.assertEquals(input, decoded);
    }

    private static FriendlyByteBuf createBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

}
