package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dev.breezes.settlements.domain.ai.naming.VillagerNameDirectory;
import dev.breezes.settlements.infrastructure.minecraft.persistence.VillagerNameDirectoryImpl;

/**
 * Binds {@link VillagerNameDirectory} to its {@code SavedData}-backed implementation.
 */
@Module
public abstract class NamingModule {

    @Binds
    abstract VillagerNameDirectory villagerNameDirectory(VillagerNameDirectoryImpl implementation);

}
