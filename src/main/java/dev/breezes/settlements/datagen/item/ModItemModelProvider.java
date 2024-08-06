package dev.breezes.settlements.datagen.item;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, SettlementsMod.MOD_ID, helper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(ItemRegistry.BASE_VILLAGER_SPAWN_EGG.getId().getPath(), mcLoc("item/template_spawn_egg"));
    }

}
