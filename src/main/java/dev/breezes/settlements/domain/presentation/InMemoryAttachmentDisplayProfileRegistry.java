package dev.breezes.settlements.domain.presentation;

import dev.breezes.settlements.domain.attachment.AttachmentSlot;
import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import lombok.Builder;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class InMemoryAttachmentDisplayProfileRegistry implements AttachmentDisplayProfileRegistry {

    private static final AttachmentDisplayProfile IDENTITY = AttachmentDisplayProfile.generic();

    private final Map<AttachmentDisplayProfileKey, AttachmentDisplayProfile> profilesByKey;

    @Builder
    private InMemoryAttachmentDisplayProfileRegistry(@Nonnull Map<AttachmentDisplayProfileKey, AttachmentDisplayProfile> profilesByKey) {
        this.profilesByKey = Map.copyOf(profilesByKey);
    }

    public static InMemoryAttachmentDisplayProfileRegistry defaults() {
        Map<AttachmentDisplayProfileKey, AttachmentDisplayProfile> profiles = new HashMap<>();

        // Hand-held tools and weapons are rendered with the item's thirdperson_righthand JSON display
        // transform so they look gripped rather than floating flat. Food, carried items, and
        // categories without an explicit profile fall back to the anchor's GROUND default.
        AttachmentDisplayProfile handTool = AttachmentDisplayProfile.builder()
                .translation(Vec3.ZERO)
                .rotation(new Vector3f())
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build();
        for (ItemCategory category : new ItemCategory[]{ItemCategory.SWORD, ItemCategory.MACE, ItemCategory.SHOVEL, ItemCategory.HOE}) {
            profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, category), handTool);
        }

        // Declare custom model display profile overrides
        profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.AXE), AttachmentDisplayProfile.builder()
                .translation(new Vec3(0.0, -0.15, -0.1))
                .rotation(new Vector3f(new Vector3f((float) Math.toRadians(-15), 0.0F, 0.0F)))
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build());
        profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.PICKAXE), AttachmentDisplayProfile.builder()
                .translation(new Vec3(0.0, -0.15, -0.1))
                .rotation(new Vector3f(new Vector3f((float) Math.toRadians(-15), 0.0F, 0.0F)))
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build());
        profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.HAMMER), AttachmentDisplayProfile.builder()
                .translation(new Vec3(0.0, -0.05, -0.15))
                .rotation(new Vector3f(new Vector3f((float) Math.toRadians(-50), 0.0F, 0.0F)))
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build());

        profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.FISHING_ROD), AttachmentDisplayProfile.builder()
                .translation(new Vec3(0.0, -0.25, -0.0))
                .rotation(new Vector3f(new Vector3f((float) Math.toRadians(-50), 0.0F, 0.0F)))
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build());

        profiles.put(AttachmentDisplayProfileKey.of(EquipmentSlot.MAIN_HAND, ItemCategory.SPYGLASS), AttachmentDisplayProfile.builder()
                .translation(new Vec3(0.1, -0.15, -0.1))
                .rotation(new Vector3f(new Vector3f((float) Math.toRadians(30), 0.0F, 0.0F)))
                .scale(1.2F)
                .displayContextOverride(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                .build());

        return InMemoryAttachmentDisplayProfileRegistry.builder()
                .profilesByKey(profiles)
                .build();
    }

    @Override
    public AttachmentDisplayProfile get(@Nonnull AttachmentSlot slot, @Nullable ItemCategory category) {
        if (category != null) {
            AttachmentDisplayProfile exact = this.profilesByKey.get(AttachmentDisplayProfileKey.of(slot, category));
            if (exact != null) {
                return exact;
            }
        }

        AttachmentDisplayProfile slotDefault = this.profilesByKey.get(AttachmentDisplayProfileKey.of(slot, ItemCategory.GENERIC));
        return slotDefault == null ? IDENTITY : slotDefault;
    }

}
