// TODO: delete this file. It's currently a reference only
//package dev.breezes.settlements.infrastructure.minecraft.ai.sensors;
//
//import dev.breezes.settlements.domain.ai.memory.MemoryTypeRegistry;
//import dev.breezes.settlements.domain.time.ClockTicks;
//import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
//import lombok.CustomLog;
//import net.minecraft.core.component.DataComponents;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.ai.memory.MemoryModuleType;
//import net.minecraft.world.entity.ai.sensing.Sensor;
//import net.minecraft.world.entity.npc.Villager;
//
//import javax.annotation.Nonnull;
//import java.util.Set;
//
//@CustomLog
//@Deprecated
//public class NeedFoodSensor extends Sensor<Villager> {
//
//    public NeedFoodSensor() {
//        super(ClockTicks.seconds(15).getTicksAsInt());
//    }
//
//    @Override
//    public Set<MemoryModuleType<?>> requires() {
//        return Set.of(MemoryTypeRegistry.NEED_FOOD.getModuleType());
//    }
//
//    @Override
//    protected void doTick(@Nonnull ServerLevel level, @Nonnull Villager villager) {
//        if (!(villager instanceof BaseVillager baseVillager)) {
//            return;
//        }
//
//        boolean needsFood = baseVillager.getSettlementsInventory().getBackpack().getItems().stream()
//                .noneMatch(itemStack -> itemStack.has(DataComponents.FOOD));
//        log.sensorStatus("Villager {} needs food? {}", villager.getStringUUID(), needsFood);
//
//        boolean hasNeedFoodMemory = villager.getBrain().hasMemoryValue(MemoryTypeRegistry.NEED_FOOD.getModuleType());
//
//        if (needsFood && !hasNeedFoodMemory) {
//            villager.getBrain().setMemory(MemoryTypeRegistry.NEED_FOOD.getModuleType(), true);
//            return;
//        }
//
//        if (!needsFood && hasNeedFoodMemory) {
//            villager.getBrain().eraseMemory(MemoryTypeRegistry.NEED_FOOD.getModuleType());
//        }
//    }
//
//}
