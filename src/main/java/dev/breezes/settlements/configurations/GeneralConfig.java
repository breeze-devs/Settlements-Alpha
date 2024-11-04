package dev.breezes.settlements.configurations;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class GeneralConfig {

    private static final ModConfigSpec.Builder CONFIG_BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue ENABLE_CLIENT = CONFIG_BUILDER
            .comment("Client functionalities enabled")
            .define("enable_client", false);

    public static final ModConfigSpec SPEC = CONFIG_BUILDER.build();

    public static boolean clientEnabled;

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        clientEnabled = ENABLE_CLIENT.get();
    }

}
