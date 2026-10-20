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

    /**
     * The mod's canonical sound for a failed interaction
     */
    public static final Supplier<SoundEvent> INTERACTION_FAIL = REGISTRY.register(
            "interaction_fail",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("interaction_fail")));

    /**
     * A settlement's bell, the sound of arriving in a settlement
     */
    public static final Supplier<SoundEvent> SETTLEMENT_ENTER = REGISTRY.register(
            "settlement_enter",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("settlement_enter")));

    /**
     * The jingle of a settlement newly known: discovered, or founded around the listener
     */
    public static final Supplier<SoundEvent> SETTLEMENT_DISCOVERY = REGISTRY.register(
            "settlement_discovery",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocationUtil.mod("settlement_discovery")));

    public static void register(IEventBus eventBus) {
        REGISTRY.register(eventBus);
    }

}
