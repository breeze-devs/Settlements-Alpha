package dev.breezes.settlements.infrastructure.network.features.ui.stats.codec;

import dev.breezes.settlements.application.ui.shared.model.SchedulePhase;
import dev.breezes.settlements.application.ui.stats.model.VillagerStatsSnapshot;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class VillagerStatsSnapshotCodecTest {

    @Test
    void roundtrip_allFieldsPopulated_preservesFields() {
        VillagerStatsSnapshot input = VillagerStatsSnapshot.builder()
                .gameTime(54321L)
                .villagerEntityId(42)
                .villagerName("Test Villager")
                .professionKey("minecraft:farmer")
                .expertiseLevel(3)
                .currentHealth(18.5F)
                .maxHealth(20.0F)
                .geneValues(new double[]{0.85, 0.42, 0.67, 0.15, 0.93, 0.50})
                .homePos(new BlockPos(100, 64, -200))
                .workstationPos(new BlockPos(105, 65, -195))
                .activeBehaviorNameKey("ui.settlements.behavior.harvest_crops")
                .activeBehaviorIconId("minecraft:wheat")
                .schedulePhase(SchedulePhase.WORK)
                .reputation(25)
                .hunger(0.72F)
                .build();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        VillagerStatsSnapshotCodec.write(buffer, input);

        VillagerStatsSnapshot decoded = VillagerStatsSnapshotCodec.read(buffer);

        Assertions.assertEquals(input.gameTime(), decoded.gameTime());
        Assertions.assertEquals(input.villagerEntityId(), decoded.villagerEntityId());
        Assertions.assertEquals(input.villagerName(), decoded.villagerName());
        Assertions.assertEquals(input.professionKey(), decoded.professionKey());
        Assertions.assertEquals(input.expertiseLevel(), decoded.expertiseLevel());
        Assertions.assertEquals(input.currentHealth(), decoded.currentHealth(), 0.001F);
        Assertions.assertEquals(input.maxHealth(), decoded.maxHealth(), 0.001F);
        Assertions.assertArrayEquals(input.geneValues(), decoded.geneValues(), 0.0001);
        Assertions.assertEquals(input.homePos(), decoded.homePos());
        Assertions.assertEquals(input.workstationPos(), decoded.workstationPos());
        Assertions.assertEquals(input.activeBehaviorNameKey(), decoded.activeBehaviorNameKey());
        Assertions.assertEquals(input.activeBehaviorIconId(), decoded.activeBehaviorIconId());
        Assertions.assertEquals(input.schedulePhase(), decoded.schedulePhase());
        Assertions.assertEquals(input.reputation(), decoded.reputation());
        Assertions.assertEquals(input.hunger(), decoded.hunger(), 0.001F);
    }

    @Test
    void roundtrip_allNullableFieldsNull_preservesNulls() {
        VillagerStatsSnapshot input = VillagerStatsSnapshot.builder()
                .gameTime(100L)
                .villagerEntityId(7)
                .villagerName(null)
                .professionKey("minecraft:none")
                .expertiseLevel(1)
                .currentHealth(20.0F)
                .maxHealth(20.0F)
                .geneValues(new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0})
                .homePos(null)
                .workstationPos(null)
                .activeBehaviorNameKey(null)
                .activeBehaviorIconId(null)
                .schedulePhase(SchedulePhase.IDLE)
                .reputation(-30)
                .hunger(1.0F)
                .build();

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        VillagerStatsSnapshotCodec.write(buffer, input);

        VillagerStatsSnapshot decoded = VillagerStatsSnapshotCodec.read(buffer);

        Assertions.assertNull(decoded.villagerName());
        Assertions.assertNull(decoded.homePos());
        Assertions.assertNull(decoded.workstationPos());
        Assertions.assertNull(decoded.activeBehaviorNameKey());
        Assertions.assertNull(decoded.activeBehaviorIconId());
        Assertions.assertEquals(input.reputation(), decoded.reputation());
    }

    @Test
    void roundtrip_geneValuesNotSharedReference() {
        double[] originalGenes = {0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
        VillagerStatsSnapshot input = VillagerStatsSnapshot.builder()
                .gameTime(1L)
                .villagerEntityId(1)
                .villagerName("Gene Test")
                .professionKey("minecraft:farmer")
                .expertiseLevel(1)
                .currentHealth(20.0F)
                .maxHealth(20.0F)
                .geneValues(originalGenes)
                .homePos(null)
                .workstationPos(null)
                .activeBehaviorNameKey(null)
                .activeBehaviorIconId(null)
                .schedulePhase(SchedulePhase.IDLE)
                .reputation(0)
                .hunger(0.5F)
                .build();

        // Mutating original array should not affect snapshot (compact constructor clones)
        originalGenes[0] = 1.0;
        Assertions.assertEquals(0.5, input.geneValues()[0], 0.0001);

        // Round-trip should also produce independent copy
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        VillagerStatsSnapshotCodec.write(buffer, input);
        VillagerStatsSnapshot decoded = VillagerStatsSnapshotCodec.read(buffer);

        Assertions.assertNotSame(input.geneValues(), decoded.geneValues());
        Assertions.assertArrayEquals(input.geneValues(), decoded.geneValues(), 0.0001);
    }

    @Test
    void roundtrip_allSchedulePhases() {
        for (SchedulePhase phase : SchedulePhase.values()) {
            VillagerStatsSnapshot input = VillagerStatsSnapshot.builder()
                    .gameTime(1L)
                    .villagerEntityId(1)
                    .villagerName("Phase Test")
                    .professionKey("minecraft:farmer")
                    .expertiseLevel(1)
                    .currentHealth(20.0F)
                    .maxHealth(20.0F)
                    .geneValues(new double[]{0, 0, 0, 0, 0, 0})
                    .homePos(null)
                    .workstationPos(null)
                    .activeBehaviorNameKey(null)
                    .activeBehaviorIconId(null)
                    .schedulePhase(phase)
                    .reputation(0)
                    .hunger(1.0F)
                    .build();

            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            VillagerStatsSnapshotCodec.write(buffer, input);
            VillagerStatsSnapshot decoded = VillagerStatsSnapshotCodec.read(buffer);

            Assertions.assertEquals(phase, decoded.schedulePhase(), "Failed for phase: " + phase);
        }
    }

}
