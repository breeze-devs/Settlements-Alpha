package dev.breezes.settlements.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.breezes.settlements.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.models.location.Location;
import dev.breezes.settlements.util.Ticks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;

import javax.annotation.Nonnull;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stest").executes(TestCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> command) {
        if (command.getSource().getEntity() instanceof Player player) {
            player.displayClientMessage(Component.literal("Starting test"), true);

            try {
                test(player);
            } catch (Exception e) {
                e.printStackTrace();
            }

            player.displayClientMessage(Component.literal("Ending test"), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static void test(@Nonnull Player player) {
        // This is a test method
        TransformedBlockDisplay display = TransformedBlockDisplay.builder()
                .blockState(Blocks.SMOOTH_STONE.defaultBlockState())
                .transform(new TransformationMatrix(
                        0.8000f, 0.0000f, 0.0000f, -0.4000f,
                        0.0000f, 0.8000f, 0.0000f, 0.1000f,
                        0.0000f, 0.0000f, 0.8000f, -0.4000f,
                        0.0000f, 0.0000f, 0.0000f, 1.0000f
                ))
                .build();
        Display displayEntity = display.spawn(Location.fromEntity(player, true));

        // Schedule a delayed run
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.schedule(() -> {
            display.setTransformation(new TransformationMatrix(
                    2.6000f, 0.0000f, 0.0000f, 0.1000f,
                    0.0000f, 0.5657f, -0.5657f, 0.1000f,
                    0.0000f, 0.5657f, 0.5657f, 0.1000f,
                    0.0000f, 0.0000f, 0.0000f, 1.0000f), Ticks.seconds(2));
        }, 2 * 1000, TimeUnit.MILLISECONDS);

        executor.schedule(display::remove, 10 * 1000, TimeUnit.MILLISECONDS);
    }

}
