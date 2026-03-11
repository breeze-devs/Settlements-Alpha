package dev.breezes.settlements.presentation.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.inventory.VillagerInventory;
import dev.breezes.settlements.domain.time.Ticks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.TransformedBlockDisplay;
import dev.breezes.settlements.infrastructure.minecraft.entities.displays.models.TransformationMatrix;
import dev.breezes.settlements.shared.util.VillagerRaycastUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stest")
                .then(Commands.literal("open_inventory").executes(TestCommand::openInventory))
                .executes(TestCommand::execute));
    }

    private static int openInventory(CommandContext<CommandSourceStack> command) {
        if (!(command.getSource().getEntity() instanceof Player player)) {
            return Command.SINGLE_SUCCESS;
        }

        Optional<EntityHitResult> hitResult = VillagerRaycastUtil.raycastVillagerTarget(player, 15.0);

        if (hitResult.isEmpty() || !(hitResult.get().getEntity() instanceof ISettlementsVillager villager)) {
            player.displayClientMessage(Component.literal("No villager found in range"), true);
            return Command.SINGLE_SUCCESS;
        }

        player.displayClientMessage(Component.literal("Opening inventory for villager: " + villager.getUUID()),
                true);

        VillagerInventory settlementsInventory = villager.getSettlementsInventory();
        SimpleContainer realBackpack = settlementsInventory.getBackpack();
        int realSize = settlementsInventory.getBackpackSize();
        SimpleContainer virtualContainer = new SimpleContainer(54);

        // Initialize virtual container
        for (int i = 0; i < 54; i++) {
            if (i < realSize) {
                virtualContainer.setItem(i, realBackpack.getItem(i));
            } else {
                ItemStack barrier = new ItemStack(Items.BARRIER);
                barrier.set(DataComponents.CUSTOM_NAME, Component.literal("Locked Slot"));
                virtualContainer.setItem(i, barrier);
            }
        }

        player.openMenu(new SimpleMenuProvider((id, inv, p) ->
                new ChestMenu(MenuType.GENERIC_9x6, id, inv, virtualContainer, 6),
                Component.literal("Villager Inventory")));

        return Command.SINGLE_SUCCESS;
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
                        0.0000f, 0.0000f, 0.0000f, 1.0000f))
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
