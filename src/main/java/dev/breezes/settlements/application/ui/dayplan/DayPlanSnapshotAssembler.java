package dev.breezes.settlements.application.ui.dayplan;

import dev.breezes.settlements.domain.ai.catalog.BehaviorDisplayMetadata;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotSnapshot;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSlotVisualStatus;
import dev.breezes.settlements.application.ui.dayplan.model.DayPlanSnapshot;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.catalog.BehaviorPlanningMetadata;
import dev.breezes.settlements.domain.ai.catalog.IBehaviorCatalog;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.time.TimeOfDay;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

@ServerScope
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class DayPlanSnapshotAssembler {

    private static final ResourceLocation DEFAULT_ICON_ITEM_ID = ResourceLocation.parse("minecraft:clock");

    private final IBehaviorCatalog behaviorCatalog;

    public DayPlanSnapshot assemble(@Nonnull DayPlan dayPlan,
                                    @Nonnull BaseVillager villager,
                                    long dayTime) {
        int currentDayTick = Math.floorMod(dayTime, TimeOfDay.TICKS_PER_DAY);
        List<DayPlanSlotSnapshot> slots = dayPlan.getSlots().stream()
                .map(slot -> this.assembleSlot(dayPlan, slot, currentDayTick))
                .toList();

        return DayPlanSnapshot.builder()
                .dayNumber(dayPlan.getGeneratedForDay())
                .dayType(dayPlan.getDayType())
                .currentTime(formatTime(currentDayTick))
                .planStatus(dayPlan.getStatus())
                .villagerEntityId(villager.getId())
                .villagerName(villager.getName().getString())
                .slots(slots)
                .build();
    }

    private DayPlanSlotSnapshot assembleSlot(@Nonnull DayPlan dayPlan, @Nonnull PlanSlot slot, int currentDayTick) {
        BehaviorDisplayMetadata displayInfo = resolveDisplayInfo(slot);

        return DayPlanSlotSnapshot.builder()
                .behaviorKey(slot.getBehaviorKey().id())
                .displayNameKey(displayInfo.displayNameKey())
                .formattedTime(formatTime(slot.getStartTick()))
                .iconItemId(displayInfo.iconItemId())
                .status(resolveVisualStatus(dayPlan, slot, currentDayTick))
                .description(resolveDescription(slot))
                .flexible(slot.isFlexible())
                .build();
    }

    private BehaviorDisplayMetadata resolveDisplayInfo(@Nonnull PlanSlot slot) {
        return this.behaviorCatalog.getDisplayInfo(slot.getBehaviorKey())
                .orElseGet(() -> BehaviorDisplayMetadata.builder()
                        .displayNameKey("ui.settlements.dayplan.behavior.unknown")
                        .iconItemId(DEFAULT_ICON_ITEM_ID)
                        .build());
    }

    @Nullable
    private String resolveDescription(@Nonnull PlanSlot slot) {
        BehaviorPlanningMetadata metadata = this.behaviorCatalog.getDescriptor(slot.getBehaviorKey()).orElse(null);
        if (metadata != null && metadata.getDescription() != null && !metadata.getDescription().isBlank()) {
            return metadata.getDescription();
        }
        return slot.getReason();
    }

    private static DayPlanSlotVisualStatus resolveVisualStatus(@Nonnull DayPlan dayPlan,
                                                               @Nonnull PlanSlot slot,
                                                               int currentDayTick) {
        if (slot.getStatus() == PlanSlotStatus.ACTIVE) {
            return DayPlanSlotVisualStatus.ACTIVE;
        }
        if (slot.getStatus() == PlanSlotStatus.COMPLETED) {
            return DayPlanSlotVisualStatus.COMPLETED;
        }
        if (slot.getStatus() == PlanSlotStatus.SKIPPED) {
            return DayPlanSlotVisualStatus.SKIPPED;
        }
        if (slot.getStatus() == PlanSlotStatus.INTERRUPTED) {
            return DayPlanSlotVisualStatus.INTERRUPTED;
        }

        int slotOffset = Math.floorMod(slot.getStartTick() - dayPlan.getDayStartTick(), TimeOfDay.TICKS_PER_DAY);
        int nowOffset = Math.floorMod(currentDayTick - dayPlan.getDayStartTick(), TimeOfDay.TICKS_PER_DAY);
        return slotOffset < nowOffset ? DayPlanSlotVisualStatus.COMPLETED : DayPlanSlotVisualStatus.UPCOMING;
    }

    private static String formatTime(int dayTick) {
        int minecraftHour = Math.floorMod(dayTick / 1000 + 6, 24);
        int minute = Math.floorMod(dayTick, 1000) * 60 / 1000;
        return String.format(Locale.ROOT, "%02d:%02d", minecraftHour, minute);
    }

}
