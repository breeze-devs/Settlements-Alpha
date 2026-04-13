package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.infrastructure.minecraft.worldgen.NbtTemplateResolver;
import lombok.CustomLog;
import lombok.NoArgsConstructor;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@NoArgsConstructor(onConstructor_ = @Inject)
@CustomLog
public final class SettlementTemplateReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected Void prepare(@Nonnull ResourceManager resourceManager, @Nonnull ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void ignored,
                         @Nonnull ResourceManager resourceManager,
                         @Nonnull ProfilerFiller profiler) {
        log.resourceLoadingStatus("Refreshing settlement template resolver after data reload");
        NbtTemplateResolver.refresh(resourceManager);
    }

}
