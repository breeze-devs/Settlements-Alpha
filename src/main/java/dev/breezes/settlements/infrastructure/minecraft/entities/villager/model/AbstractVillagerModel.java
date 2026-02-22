package dev.breezes.settlements.infrastructure.minecraft.entities.villager.model;

import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;

public abstract class AbstractVillagerModel<T extends BaseVillager> extends HierarchicalModel<T> implements HeadedModel, ArmedModel {
}
