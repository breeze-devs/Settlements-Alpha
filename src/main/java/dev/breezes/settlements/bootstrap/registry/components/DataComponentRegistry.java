package dev.breezes.settlements.bootstrap.registry.components;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponentRegistry {

    public static final DeferredRegister<DataComponentType<?>> REGISTRY =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, SettlementsMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> VILLAGER_ENCHANT_ATTEMPTED =
            REGISTRY.register("enchant_attempted", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> VILLAGER_TOTEM_MODE =
            REGISTRY.register("villager_totem_mode", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.INT)
                    .build());

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
