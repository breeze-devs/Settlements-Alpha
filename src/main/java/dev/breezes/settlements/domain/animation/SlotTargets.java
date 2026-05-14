package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class SlotTargets {

    private static final Map<AttachmentSlot, AnimationTarget<Vec3>> TRANSLATIONS = new ConcurrentHashMap<>();
    private static final Map<AttachmentSlot, AnimationTarget<Vector3f>> ROTATIONS = new ConcurrentHashMap<>();
    private static final Map<AttachmentSlot, AnimationTarget<Float>> SCALES = new ConcurrentHashMap<>();
    private static final Map<AttachmentSlot, AnimationTarget<Boolean>> VISIBILITIES = new ConcurrentHashMap<>();
    private static final Map<AttachmentSlot, AnimationTarget<ItemDisplayContext>> DISPLAY_CONTEXTS = new ConcurrentHashMap<>();

    public static AnimationTarget<Vec3> translation(@Nonnull AttachmentSlot slot) {
        return TRANSLATIONS.computeIfAbsent(slot, ignored -> AnimationTarget.<Vec3>builder()
                .id(slotTargetId(slot, "translation"))
                .valueType(Vec3.class)
                .neutralValue(Vec3.ZERO)
                .interpolator(Interpolators.VEC3)
                .policy(AnimationTargetPolicy.ADDITIVE)
                .build());
    }

    public static AnimationTarget<Vector3f> rotation(@Nonnull AttachmentSlot slot) {
        return ROTATIONS.computeIfAbsent(slot, ignored -> AnimationTarget.<Vector3f>builder()
                .id(slotTargetId(slot, "rotation"))
                .valueType(Vector3f.class)
                .neutralValue(new Vector3f())
                .interpolator(Interpolators.VECTOR3F)
                .policy(AnimationTargetPolicy.ADDITIVE)
                .build());
    }

    public static AnimationTarget<Float> scale(@Nonnull AttachmentSlot slot) {
        return SCALES.computeIfAbsent(slot, ignored -> AnimationTarget.<Float>builder()
                .id(slotTargetId(slot, "scale"))
                .valueType(Float.class)
                .neutralValue(1.0F)
                .interpolator(Interpolators.FLOAT)
                .policy(AnimationTargetPolicy.MULTIPLICATIVE)
                .build());
    }

    public static AnimationTarget<Boolean> visibility(@Nonnull AttachmentSlot slot) {
        return VISIBILITIES.computeIfAbsent(slot, ignored -> AnimationTarget.<Boolean>builder()
                .id(slotTargetId(slot, "visibility"))
                .valueType(Boolean.class)
                .neutralValue(true)
                .interpolator(Interpolators.BOOLEAN_STEP)
                .policy(AnimationTargetPolicy.ABSOLUTE)
                .build());
    }

    public static AnimationTarget<ItemDisplayContext> displayContext(@Nonnull AttachmentSlot slot) {
        return DISPLAY_CONTEXTS.computeIfAbsent(slot, ignored -> AnimationTarget.<ItemDisplayContext>builder()
                .id(slotTargetId(slot, "display_context"))
                .valueType(ItemDisplayContext.class)
                .neutralValue(ItemDisplayContext.NONE)
                .interpolator(Interpolators.ITEM_DISPLAY_CONTEXT_STEP)
                .policy(AnimationTargetPolicy.ABSOLUTE)
                .build());
    }

    private static String slotTargetId(@Nonnull AttachmentSlot slot, @Nonnull String property) {
        return "slot:" + slot.getClass().getName() + ":" + slot + "." + property;
    }

}
