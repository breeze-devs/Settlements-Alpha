package dev.breezes.settlements.infrastructure.minecraft.entities.pet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Water-avoiding random stroll that keeps a pet in or near a village.
 */
public class PetVillageBoundStrollGoal extends WaterAvoidingRandomStrollGoal {

    private static final int MAX_HORIZONTAL_DISTANCE = 10;
    private static final int MAX_VERTICAL_DISTANCE = 7;
    private static final int VILLAGE_SEARCH_RADIUS_SECTIONS = 2;
    private static final double TOWARD_VILLAGE_MAX_ANGLE = Math.PI / 2;

    private final TamableAnimal pet;

    public PetVillageBoundStrollGoal(@Nonnull TamableAnimal pet, double speedModifier) {
        super(pet, speedModifier);
        this.pet = pet;
    }

    @Nullable
    @Override
    protected Vec3 getPosition() {
        // A loaded owner already anchors the pet through following, so return the normal random stroll destination
        if (this.pet.getOwner() != null || this.pet.isInWaterOrBubble() || !(this.pet.level() instanceof ServerLevel level)) {
            return super.getPosition();
        }

        BlockPos position = this.pet.blockPosition();
        if (level.isVillage(position)) {
            return super.getPosition();
        }

        SectionPos section = SectionPos.of(position);
        SectionPos closerSection = BehaviorUtils.findSectionClosestToVillage(level, section, VILLAGE_SEARCH_RADIUS_SECTIONS);
        if (closerSection.equals(section)) {
            return super.getPosition();
        }

        return DefaultRandomPos.getPosTowards(this.pet, MAX_HORIZONTAL_DISTANCE, MAX_VERTICAL_DISTANCE,
                Vec3.atBottomCenterOf(closerSection.center()), TOWARD_VILLAGE_MAX_ANGLE);
    }

}
