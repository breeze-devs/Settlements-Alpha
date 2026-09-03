package dev.breezes.settlements.infrastructure.rendering.debug;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.fml.loading.FMLLoader;

/**
 * Whether developer-only surfaces may install themselves.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DevTooling {

    public static boolean isEnabled() {
        return !FMLLoader.isProduction();
    }

}
