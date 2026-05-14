package dev.breezes.settlements.domain.inventory;

import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.Set;

public interface IVillagerEquipment {

    Optional<ItemStack> getEquipped(@Nonnull EquipmentSlot slot);

    Set<EquipmentSlot> occupiedSlots();

}
