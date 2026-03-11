package dev.breezes.settlements.shared.util;

import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Optional;

public final class VillagerRaycastUtil {

    public static Optional<EntityHitResult> raycastVillagerTarget(@Nonnull Player player, double reach) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(reach));
        AABB aabb = player.getBoundingBox().expandTowards(lookVec.scale(reach)).inflate(1.0D);

        return Optional.ofNullable(ProjectileUtil.getEntityHitResult(player, eyePos, endPos, aabb, VillagerRaycastUtil::isVillager, reach * reach));
    }

    private static boolean isVillager(Entity entity) {
        return entity instanceof ISettlementsVillager;
    }

}
