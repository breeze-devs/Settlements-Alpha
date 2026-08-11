package dev.breezes.settlements.presentation.ui.hud;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CultivationLilyLookTargetHudProvider#filterGestureKey} is the only pure decision in content
 * assembly here — everything else builds a {@link net.minecraft.network.chat.Component}, which is left for
 * manual verification per this project's established constraint on Minecraft in unit tests.
 */
class CultivationLilyLookTargetHudProviderTest {

    @Test
    void filterGestureKey_noFilter_namesOnlyTheGestureThatSetsOne() {
        // Naming the clear gesture here would teach a rule the player cannot act on: there is nothing to clear.
        assertEquals("ui.settlements.lily_hud.filter_set_gesture",
                CultivationLilyLookTargetHudProvider.filterGestureKey(null));
    }

    @Test
    void filterGestureKey_filterPresent_namesTheGesturesThatChangeIt() {
        // A lily that already has a filter is the only state from which clearing and replacing are reachable,
        // so a swapped branch here leaves the player with no on-screen route back to an unfiltered zone.
        ResourceLocation wheat = ResourceLocation.parse("minecraft:wheat");

        assertEquals("ui.settlements.lily_hud.filter_change_gesture",
                CultivationLilyLookTargetHudProvider.filterGestureKey(wheat));
    }

}
