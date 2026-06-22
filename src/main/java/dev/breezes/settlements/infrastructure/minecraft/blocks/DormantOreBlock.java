package dev.breezes.settlements.infrastructure.minecraft.blocks;

import dev.breezes.settlements.bootstrap.registry.particles.ParticleRegistry;
import dev.breezes.settlements.di.SettlementsDagger;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.infrastructure.config.factory.ConfigFactory;
import dev.breezes.settlements.infrastructure.minecraft.data.mining.OreRegenDataManager;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

@Getter
public class DormantOreBlock extends Block {

    /**
     * Identifies which host stratum this node sits in
     * <p>
     * The data manager filters the weighted table by this value so only matching
     * (or "any") ore entries are eligible — a stone node can never spawn deepslate ore
     */
    public enum Host {
        STONE, DEEPSLATE
    }

    private final Host host;

    public DormantOreBlock(@Nonnull BlockBehaviour.Properties properties, @Nonnull Host host) {
        super(properties);
        this.host = host;
    }

    /**
     * The expected recharge time emerges from the chance combined with the random tick speed
     */
    @Override
    public void randomTick(@Nonnull BlockState state,
                           @Nonnull ServerLevel level,
                           @Nonnull BlockPos pos,
                           @Nonnull RandomSource random) {
        OreRegenConfig config = ConfigFactory.create(OreRegenConfig.class);
        if (!config.enabled()) {
            return;
        }

        if (random.nextDouble() >= config.regenChancePerRandomTick()) {
            return;
        }

        OreRegenDataManager dataManager = SettlementsDagger.serverOrThrow().oreRegenDataManager();
        dataManager.rollForHost(this.host)
                .flatMap(dataManager::resolveBlockState)
                .ifPresent(rechargedState -> {
                    level.setBlockAndUpdate(pos, rechargedState);
                    playRechargeEffects(pos, level, rechargedState);
                });
    }

    private static void playRechargeEffects(@Nonnull BlockPos pos,
                                            @Nonnull ServerLevel level,
                                            @Nonnull BlockState rechargedState) {
        Location effectLocation = Location.of(pos, level).center(true);
        ParticleRegistry.oreRecharge(effectLocation, rechargedState);
        effectLocation.playSound(rechargedState.getSoundType(level, pos, null).getPlaceSound(),
                0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F, SoundSource.BLOCKS);
    }

}
