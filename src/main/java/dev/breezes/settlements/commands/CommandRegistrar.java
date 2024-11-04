package dev.breezes.settlements.commands;

import dev.breezes.settlements.SettlementsMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class CommandRegistrar {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        TestCommand.register(event.getDispatcher());
    }

}
