package dev.breezes.settlements.di.modules.client;

import dagger.Module;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.presentation.ui.hud.LookTargetHudProvider;

import java.util.Set;

/**
 * Declares the crosshair HUD region's look-target layer so its owner can depend on
 * {@code Set<LookTargetHudProvider>} before any feature contributes to it.
 */
@Module
public abstract class HudModule {

    @Multibinds
    abstract Set<LookTargetHudProvider> lookTargetHudProviders();

}
