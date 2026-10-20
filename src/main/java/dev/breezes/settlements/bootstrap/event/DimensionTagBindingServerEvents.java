package dev.breezes.settlements.bootstrap.event;

import dev.breezes.settlements.SettlementsMod;
import lombok.CustomLog;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.world.level.dimension.LevelStem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Binds the data packs' dimension tags to the dimensions the server runs, before any level loads, so that a dimension
 * tag reads the same on every start (platform standard P19).
 */
@EventBusSubscriber(modid = SettlementsMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
@CustomLog
public final class DimensionTagBindingServerEvents {

    // TODO: delete this class when a Minecraft upgrade binds dimension tags on a newly created world's first start;
    //  check on each upgrade, by founding at once in a new single-player world
    @SubscribeEvent
    public static void onServerAboutToStart(@Nonnull ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        Registry<LevelStem> dimensions = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        // Loaded and bound as a reload does, against this server's own registry. On a start whose tags are already
        // bound, this binds the same tags again.
        Map<TagKey<LevelStem>, List<Holder<LevelStem>>> tags =
                new TagLoader<Holder<LevelStem>>(dimensions::getHolder, Registries.tagsDirPath(Registries.LEVEL_STEM))
                        .loadAndBuild(server.getResourceManager())
                        .entrySet()
                        .stream()
                        .collect(Collectors.toUnmodifiableMap(
                                entry -> TagKey.create(Registries.LEVEL_STEM, entry.getKey()),
                                entry -> List.copyOf(entry.getValue())));
        dimensions.bindTags(tags);
        log.info("Bound {} dimension tags to the server's dimensions", tags.size());
    }

}
