package dev.breezes.settlements.presentation.commands;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class CommandRegistrar {

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        SettlementsCommand.register(event.getDispatcher());
        TestCommand.register(event.getDispatcher());
    }

}
