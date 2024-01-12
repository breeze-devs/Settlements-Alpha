package dev.breezes.settlements.registry;

import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class CreativeTabRegistry {

    public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SettlementsMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SETTLEMENTS_TAB = REGISTRY.register("settlements", () -> CreativeModeTab.builder()
            .title(Component.translatable("creative_tab.settlements"))
            .icon(() -> ItemRegistry.CORN.get().getDefaultInstance())
            .displayItems(((pParameters, pOutput) -> {
                pOutput.accept(ItemRegistry.SAPPHIRE.get());
                pOutput.accept(ItemRegistry.RAW_SAPPHIRE.get());
                pOutput.accept(ItemRegistry.METAL_DETECTOR.get());
                pOutput.accept(ItemRegistry.SAPPHIRE_STAFF.get());

                pOutput.accept(ItemRegistry.SAPPHIRE_HELMET.get());
                pOutput.accept(ItemRegistry.SAPPHIRE_CHESTPLATE.get());
                pOutput.accept(ItemRegistry.SAPPHIRE_LEGGINGS.get());
                pOutput.accept(ItemRegistry.SAPPHIRE_BOOTS.get());

                pOutput.accept(BlockRegistry.SAPPHIRE_BLOCK.get());
                pOutput.accept(BlockRegistry.RAW_SAPPHIRE_BLOCK.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_ORE.get());

                pOutput.accept(BlockRegistry.SAPPHIRE_STAIRS.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_BUTTON.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_PRESSURE_PLATE.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_DOOR.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_TRAPDOOR.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_FENCE.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_FENCE_GATE.get());
                pOutput.accept(BlockRegistry.SAPPHIRE_WALL.get());

                pOutput.accept(ItemRegistry.STRAWBERRY_SEEDS.get());
                pOutput.accept(ItemRegistry.STRAWBERRY.get());

                pOutput.accept(ItemRegistry.BLUEBERRY_SEEDS.get());
                pOutput.accept(ItemRegistry.BLUEBERRY.get());

                pOutput.accept(ItemRegistry.CORN_SEEDS.get());
                pOutput.accept(ItemRegistry.CORN.get());

                pOutput.accept(BlockRegistry.CAT_MINT.get());

                pOutput.accept(ItemRegistry.RHINO_SPAWN_EGG.get());
                pOutput.accept(ItemRegistry.BASE_VILLAGER_SPAWN_EGG.get());
            }))
            .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
