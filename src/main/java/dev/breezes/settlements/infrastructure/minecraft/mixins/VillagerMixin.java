package dev.breezes.settlements.infrastructure.minecraft.mixins;

import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Villager.class)
public interface VillagerMixin {

    @Accessor("increaseProfessionLevelOnUpdate")
    void setIncreaseProfessionLevelOnUpdate(boolean shouldLevelUp);

    @Accessor("updateMerchantTimer")
    void setUpdateMerchantTimer(int levelUpCooldown);

    @Invoker("shouldIncreaseLevel")
    boolean invokeShouldIncreaseLevel();

}
