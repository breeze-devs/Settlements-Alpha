package dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.mixins.VillagerMixin;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerConversionUtil {

    public static void convertToVanilla(@Nonnull ServerLevel level,
                                        @Nonnull Villager oldVillager,
                                        boolean stasis) {
        if (!(oldVillager instanceof BaseVillager)) {
            applyVanillaState(oldVillager, stasis);
            return;
        }

        CompoundTag tag = copyEntityTagWithoutIdentity(oldVillager);
        Villager newVillager = EntityType.VILLAGER.create(level);
        if (newVillager == null) {
            return;
        }

        newVillager.load(tag);
        copyPose(oldVillager, newVillager);
        applyVanillaState(newVillager, stasis);

        level.addFreshEntity(newVillager);

        releasePoiTickets(oldVillager);
        oldVillager.discard();
    }

    @Nullable
    public static BaseVillager convertToSettlements(@Nonnull ServerLevel level,
                                                    @Nonnull Villager oldVillager) {
        if (oldVillager instanceof BaseVillager) {
            applyVanillaState(oldVillager, false);
            return null;
        }

        CompoundTag tag = copyEntityTagWithoutIdentity(oldVillager);
        BaseVillager newVillager = EntityRegistry.BASE_VILLAGER.get().create(level);
        if (newVillager == null) {
            return null;
        }

        newVillager.load(tag);
        copyPose(oldVillager, newVillager);
        applyVanillaState(newVillager, false);

        level.addFreshEntity(newVillager);

        releasePoiTickets(oldVillager);
        oldVillager.discard();

        return newVillager;
    }

    private static void releasePoiTickets(@Nonnull Villager villager) {
        ((VillagerMixin) villager).invokeReleaseAllPois();
    }

    private static CompoundTag copyEntityTagWithoutIdentity(@Nonnull Villager villager) {
        CompoundTag tag = new CompoundTag();
        villager.saveWithoutId(tag);
        tag.remove("UUID");
        return tag;
    }

    private static void copyPose(@Nonnull Villager oldVillager,
                                 @Nonnull Villager newVillager) {
        newVillager.setPos(oldVillager.position());
        newVillager.setYRot(oldVillager.getYRot());
        newVillager.setXRot(oldVillager.getXRot());
    }

    private static void applyVanillaState(@Nonnull Villager villager,
                                          boolean stasis) {
        villager.setNoAi(stasis);
        villager.setSilent(stasis);
    }

}
