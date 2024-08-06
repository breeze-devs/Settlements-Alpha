package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CreativeTabRegistry {

    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SettlementsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SETTLEMENTS_TAB = REGISTRY.register("settlements", () -> CreativeModeTab.builder()
            .title(Component.translatable("creative_tab.settlements"))
            .icon(Items.EMERALD::getDefaultInstance)
            .displayItems(((pParameters, pOutput) -> {
                pOutput.accept(ItemRegistry.BASE_VILLAGER_SPAWN_EGG.get());
            }))
            .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
