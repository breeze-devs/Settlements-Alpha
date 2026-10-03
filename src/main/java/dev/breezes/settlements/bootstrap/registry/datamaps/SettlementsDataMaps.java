package dev.breezes.settlements.bootstrap.registry.datamaps;

import com.mojang.serialization.Codec;
import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Registry data maps this mod defines.
 * JSON lives under data/settlements/data_maps/.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SettlementsDataMaps {

    /**
     * Per-entity-type danger rating a threat assessment weighs an aware hostile by. Not synced to clients.
     */
    @ServerSide
    public static final DataMapType<EntityType<?>, Float> THREAT_WEIGHTS = DataMapType.builder(
            ResourceLocationUtil.mod("threat_weights"),
            Registries.ENTITY_TYPE,
            Codec.floatRange(0.0F, Float.MAX_VALUE)).build();

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent event) {
        event.register(THREAT_WEIGHTS);
    }

}
