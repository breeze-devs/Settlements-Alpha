package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.planning.DayPlan;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityBlock;
import dev.breezes.settlements.domain.ai.planning.DayPlanActivityContext;
import dev.breezes.settlements.domain.ai.planning.DayPlanSchedule;
import dev.breezes.settlements.domain.ai.planning.PlanSlot;
import dev.breezes.settlements.domain.ai.planning.PlanSlotStatus;
import dev.breezes.settlements.domain.ai.planning.PlanStatus;
import dev.breezes.settlements.domain.ai.schedule.PlanDayType;

public final class DayPlanAttachmentCodec {

    private static final Codec<BehaviorKey> BEHAVIOR_KEY_CODEC = Codec.STRING.xmap(BehaviorKey::of, BehaviorKey::id);
    private static final Codec<PlanStatus> PLAN_STATUS_CODEC = enumCodec(PlanStatus.class);
    private static final Codec<PlanSlotStatus> PLAN_SLOT_STATUS_CODEC = enumCodec(PlanSlotStatus.class)
            .xmap(DayPlanAttachmentCodec::normalizeSlotStatus, status -> status);
    private static final Codec<PlanDayType> PLAN_DAY_TYPE_CODEC = enumCodec(PlanDayType.class);
    private static final Codec<DayPlanActivityContext> DAY_PLAN_ACTIVITY_CONTEXT_CODEC = enumCodec(DayPlanActivityContext.class);

    private static final Codec<DayPlanActivityBlock> DAY_PLAN_ACTIVITY_BLOCK_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DAY_PLAN_ACTIVITY_CONTEXT_CODEC.fieldOf("context").forGetter(DayPlanActivityBlock::context),
                    Codec.INT.fieldOf("startTick").forGetter(DayPlanActivityBlock::startTick),
                    Codec.INT.fieldOf("endTick").forGetter(DayPlanActivityBlock::endTick),
                    Codec.STRING.optionalFieldOf("reason", "").forGetter(DayPlanActivityBlock::reason)
            ).apply(instance, DayPlanActivityBlock::new));

    private static final Codec<DayPlanSchedule> DAY_PLAN_SCHEDULE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("wakeTick").forGetter(DayPlanSchedule::wakeTick),
                    Codec.INT.fieldOf("bedtimeTick").forGetter(DayPlanSchedule::bedtimeTick),
                    DAY_PLAN_ACTIVITY_BLOCK_CODEC.listOf().fieldOf("activityBlocks").forGetter(DayPlanSchedule::activityBlocks)
            ).apply(instance, DayPlanSchedule::new));

    private static final Codec<PlanSlot> PLAN_SLOT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("startTick").forGetter(PlanSlot::getStartTick),
                    BEHAVIOR_KEY_CODEC.fieldOf("behaviorKey").forGetter(PlanSlot::getBehaviorKey),
                    Codec.INT.fieldOf("priority").forGetter(PlanSlot::getPriority),
                    Codec.BOOL.fieldOf("flexible").forGetter(PlanSlot::isFlexible),
                    Codec.INT.fieldOf("estimatedDurationTicks").forGetter(PlanSlot::getEstimatedDurationTicks),
                    Codec.STRING.optionalFieldOf("reason", "").forGetter(PlanSlot::getReason),
                    PLAN_SLOT_STATUS_CODEC.optionalFieldOf("status", PlanSlotStatus.PENDING).forGetter(PlanSlot::getStatus)
            ).apply(instance, PlanSlot::new));

    public static final Codec<DayPlan> DAY_PLAN_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PLAN_SLOT_CODEC.listOf().fieldOf("slots").forGetter(DayPlan::getSlots),
                    PLAN_DAY_TYPE_CODEC.fieldOf("dayType").forGetter(DayPlan::getDayType),
                    Codec.LONG.fieldOf("generatedForDay").forGetter(DayPlan::getGeneratedForDay),
                    DAY_PLAN_SCHEDULE_CODEC.fieldOf("schedule").forGetter(DayPlan::getSchedule),
                    PLAN_STATUS_CODEC.optionalFieldOf("status", PlanStatus.PENDING)
                            .forGetter(plan -> normalizePlanStatus(plan.getStatus())),
                    Codec.INT.optionalFieldOf("currentSlotIndex", 0).forGetter(DayPlan::getCurrentSlotIndex),
                    Codec.INT.optionalFieldOf("dayStartTick", 0).forGetter(DayPlan::getDayStartTick)
            ).apply(instance, DayPlan::new));

    public static final Codec<DayPlanAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    DAY_PLAN_CODEC.optionalFieldOf("plan").forGetter(DayPlanAttachmentState::plan)
            ).apply(instance, DayPlanAttachmentState::new));

    private DayPlanAttachmentCodec() {
    }

    private static PlanSlotStatus normalizeSlotStatus(PlanSlotStatus status) {
        // Behavior instances are transient and cannot be reconstructed mid-tick from saved data.
        // Restarting active slots preserves intent without pretending an inner behavior survived unload.
        return status == PlanSlotStatus.ACTIVE ? PlanSlotStatus.PENDING : status;
    }

    private static PlanStatus normalizePlanStatus(PlanStatus status) {
        return switch (status) {
            case ACTIVE, SUSPENDED -> PlanStatus.PENDING;
            case PENDING, COMPLETED -> status;
        };
    }

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> enumClass) {
        return Codec.STRING.comapFlatMap(
                value -> parseEnum(enumClass, value),
                Enum::name);
    }

    private static <E extends Enum<E>> DataResult<E> parseEnum(Class<E> enumClass, String value) {
        try {
            return DataResult.success(Enum.valueOf(enumClass, value));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown " + enumClass.getSimpleName() + " value: " + value);
        }
    }

}
