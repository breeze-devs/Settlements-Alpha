package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.domain.inventory.EquipmentSlot;
import dev.breezes.settlements.domain.inventory.IVillagerEquipment;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public final class EquipmentLookup {

    public static Optional<IVillagerEquipment> find(@Nonnull BaseVillager villager) {
        // Client entities are constructed from spawn packets and never go through load() or finalizeSpawn(),
        // so settlementsInventory is always null on the client. The bridge handles this by falling through
        // to vanilla-synced hand slots, which are always available on both sides.
        @Nullable VillagerInventory inventory = villager.hasSettlementsInventory() ? villager.getSettlementsInventory() : null;
        return Optional.of(new VanillaHandBridgedEquipment(villager, inventory));
    }

    private static final class VanillaHandBridgedEquipment implements IVillagerEquipment {

        private final BaseVillager villager;
        @Nullable
        private final VillagerInventory inventory;

        VanillaHandBridgedEquipment(@Nonnull BaseVillager villager, @Nullable VillagerInventory inventory) {
            this.villager = villager;
            this.inventory = inventory;
        }

        @Override
        public Optional<ItemStack> getEquipped(@Nonnull EquipmentSlot slot) {
            if (this.inventory != null) {
                Optional<ItemStack> equipped = this.inventory.getEquipped(slot);
                if (equipped.isPresent()) {
                    return equipped;
                }
            }
            // Server-side inventory writes don't reach the client entity's in-memory equipped map;
            // vanilla hand slots are always synced and serve as the authoritative fallback.
            ItemStack vanillaStack = switch (slot) {
                case MAIN_HAND -> this.villager.getItemInHand(InteractionHand.MAIN_HAND);
                case OFF_HAND -> this.villager.getItemInHand(InteractionHand.OFF_HAND);
            };
            return vanillaStack.isEmpty() ? Optional.empty() : Optional.of(vanillaStack);
        }

        @Override
        public Set<EquipmentSlot> occupiedSlots() {
            EnumSet<EquipmentSlot> occupied = EnumSet.noneOf(EquipmentSlot.class);
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (this.getEquipped(slot).isPresent()) {
                    occupied.add(slot);
                }
            }
            return Collections.unmodifiableSet(occupied);
        }
    }

}
