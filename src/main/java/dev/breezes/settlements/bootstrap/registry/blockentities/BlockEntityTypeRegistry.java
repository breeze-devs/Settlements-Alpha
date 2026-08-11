package dev.breezes.settlements.bootstrap.registry.blockentities;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.bootstrap.registry.blocks.BlockRegistry;
import dev.breezes.settlements.infrastructure.minecraft.blocks.cultivation.CultivationLilyBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class BlockEntityTypeRegistry {

    public static final DeferredRegister<BlockEntityType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SettlementsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CultivationLilyBlockEntity>> CULTIVATION_LILY =
            REGISTRY.register("cultivation_lily",
                    () -> BlockEntityType.Builder
                            .of(CultivationLilyBlockEntity::new, BlockRegistry.CULTIVATION_LILY.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
