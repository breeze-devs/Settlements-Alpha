package dev.breezes.settlements.domain.ai.sensors;


import net.minecraft.world.entity.Entity;

public interface INearbySensor<T extends Entity> extends ISensor<T> {

    int getHorizontalRange();

    int getVerticalRange();

}
