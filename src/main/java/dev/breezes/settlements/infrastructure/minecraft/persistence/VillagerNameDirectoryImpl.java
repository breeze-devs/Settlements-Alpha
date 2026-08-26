package dev.breezes.settlements.infrastructure.minecraft.persistence;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Holds no state -- every call resolves {@link VillagerNameSavedData} for the current server fresh.
 */
@ServerScope
@NoArgsConstructor(onConstructor_ = @Inject)
public final class VillagerNameDirectoryImpl implements VillagerNameDirectory {

    @Override
    public String resolve(@Nonnull UUID uuid) {
        return savedData().resolve(uuid);
    }

    @Override
    public void upsert(@Nonnull UUID uuid, @Nonnull String name) {
        savedData().upsert(uuid, name);
    }

    @Override
    public void remove(@Nonnull UUID uuid) {
        savedData().remove(uuid);
    }

    private static VillagerNameSavedData savedData() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("VillagerNameDirectory used while no MinecraftServer is running");
        }

        return VillagerNameSavedData.get(server);
    }

}
