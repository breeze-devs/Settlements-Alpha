package dev.breezes.settlements.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.breezes.settlements.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.models.location.Location;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix4f;

import javax.annotation.Nonnull;


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
                .blockState(Blocks.SMOOTH_BASALT.defaultBlockState())
                .transform(new Matrix4f(
                        0.6055f, 0.0000f, 0.0000f, 0.1973f,
                        0.0000f, 0.0938f, 0.0000f, 0.0000f,
                        0.0000f, 0.0000f, 0.6055f, 0.1973f,
                        0.0000f, 0.0000f, 0.0000f, 1.0000f
                ).transpose())
                .build();
        display.createEntity(Location.fromEntity(player, true));
    }

}
