package dev.breezes.settlements.bootstrap.registry.sounds;

import dev.breezes.settlements.SettlementsMod;
import dev.breezes.settlements.shared.util.ResourceLocationUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class SoundEventRegistry {

    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SettlementsMod.MOD_ID);

    // Variable-range so the client can attenuate the crow over distance naturally
    public static final Supplier<SoundEvent> CUCCO_CALL = REGISTRY.register(
            "cucco_call",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("cucco_call")));

    public static final Supplier<SoundEvent> CUCCO_FALL = REGISTRY.register(
            "cucco_fall",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("cucco_fall")));

    public static final Supplier<SoundEvent> WOLOLO = REGISTRY.register(
            "wololo",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("wololo")));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
