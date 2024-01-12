package dev.breezes.settlements.registry;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class FoodRegistry {

    public static final FoodProperties STRAWBERRY = new FoodProperties.Builder()
            .nutrition(2)
            .saturationMod(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 2 * 20, 0), 0.5F)
            .fast()
            .build();

    public static final FoodProperties CORN = new FoodProperties.Builder()
            .nutrition(3)
            .saturationMod(0.4F)
            .build();

}
