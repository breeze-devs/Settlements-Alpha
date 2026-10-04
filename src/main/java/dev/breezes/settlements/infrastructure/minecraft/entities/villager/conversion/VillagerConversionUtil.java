package dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion;

import dev.breezes.settlements.bootstrap.registry.entities.EntityRegistry;
import dev.breezes.settlements.domain.personality.OriginType;
import dev.breezes.settlements.infrastructure.minecraft.attachments.VillagerOriginAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Converts a villager between the vanilla and Settlements entity types, waking it if asleep.
 * <p>
 * A conversion replaces the villager with a copy loaded from its saved data under a new UUID, which keeps the home,
 * job site, and meeting point the villager held reserved.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VillagerConversionUtil {

    /**
     * Replaces a Settlements villager with a vanilla copy, frozen and silenced when stasis is set.
     * A vanilla villager is frozen or unfrozen in place instead.
     * <p>
     * If the copy cannot be created or the level refuses it, the Settlements villager stays unconverted.
     */
    public static void convertToVanilla(@Nonnull ServerLevel level,
                                        @Nonnull Villager oldVillager,
                                        boolean stasis) {
        if (!(oldVillager instanceof BaseVillager)) {
            wake(oldVillager);
            applyVanillaState(oldVillager, stasis);
            return;
        }

        Villager newVillager = EntityType.VILLAGER.create(level);
        if (newVillager == null) {
            return;
        }

        settle(level, oldVillager);
        newVillager.load(copyEntityTagWithoutIdentity(oldVillager));
        copyPose(oldVillager, newVillager);
        applyVanillaState(newVillager, stasis);

        replace(level, oldVillager, newVillager);
    }

    /**
     * Replaces a vanilla villager with an unfrozen Settlements copy stamped with origin.
     * A Settlements villager is unfrozen in place instead.
     *
     * @return the copy, or null when the old villager remains: it was already a Settlements villager, or the copy
     * could not be created or was refused by the level
     */
    @Nullable
    public static BaseVillager convertToSettlements(@Nonnull ServerLevel level,
                                                    @Nonnull Villager oldVillager,
                                                    @Nonnull OriginType origin) {
        if (oldVillager instanceof BaseVillager) {
            wake(oldVillager);
            applyVanillaState(oldVillager, false);
            return null;
        }

        BaseVillager newVillager = EntityRegistry.BASE_VILLAGER.get().create(level);
        if (newVillager == null) {
            return null;
        }

        settle(level, oldVillager);
        newVillager.load(copyEntityTagWithoutIdentity(oldVillager));
        copyPose(oldVillager, newVillager);
        applyVanillaState(newVillager, false);
        VillagerOriginAttachment.stamp(newVillager, origin);

        return replace(level, oldVillager, newVillager) ? newVillager : null;
    }

    private static void settle(@Nonnull ServerLevel level,
                               @Nonnull Villager villager) {
        // End behaviors and run teardown automatically
        villager.getBrain().stopAll(level, villager);

        // Stopping wakes only a villager whose sleep behavior is running, and one loaded asleep has none
        wake(villager);
    }

    private static void wake(@Nonnull Villager villager) {
        if (villager.isSleeping()) {
            villager.stopSleeping();
        }
    }

    /**
     * Adds newVillager to the level and discards oldVillager, or keeps oldVillager when the level refuses newVillager.
     *
     * @return whether newVillager entered the level
     */
    private static boolean replace(@Nonnull ServerLevel level,
                                   @Nonnull Villager oldVillager,
                                   @Nonnull Villager newVillager) {
        if (!level.addFreshEntity(newVillager)) {
            // Settling releases no home, job site, or meeting point, so the kept oldVillager needs no rollback
            return false;
        }

        // Nothing is released: the tickets oldVillager held now back the identical claims in newVillager's
        // memories, and freeing them would let a neighbor take its bed or workstation
        oldVillager.discard();
        return true;
    }

    private static CompoundTag copyEntityTagWithoutIdentity(@Nonnull Villager villager) {
        CompoundTag tag = new CompoundTag();
        villager.saveWithoutId(tag);
        // The copy enters the level before the original leaves it, and the level refuses a duplicate UUID
        tag.remove("UUID");
        return tag;
    }

    private static void copyPose(@Nonnull Villager oldVillager,
                                 @Nonnull Villager newVillager) {
        newVillager.setPos(oldVillager.position());
        newVillager.setYRot(oldVillager.getYRot());
        newVillager.setXRot(oldVillager.getXRot());
        // Loading the saved data turns body and head to face along the yaw
        newVillager.setYBodyRot(oldVillager.yBodyRot);
        newVillager.setYHeadRot(oldVillager.getYHeadRot());
    }

    private static void applyVanillaState(@Nonnull Villager villager,
                                          boolean stasis) {
        villager.setNoAi(stasis);
        villager.setSilent(stasis);
    }

}
