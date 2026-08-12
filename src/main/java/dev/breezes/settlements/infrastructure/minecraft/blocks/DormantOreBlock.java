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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

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

    private static final int INITIAL_STAGE = 0;
    private static final int FINAL_STAGE = 1;

    /**
     * How far along the node is: bare rock at {@link #INITIAL_STAGE}, ore visibly forming at {@link #FINAL_STAGE}
     */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", INITIAL_STAGE, FINAL_STAGE);

    private final Host host;

    public DormantOreBlock(@Nonnull BlockBehaviour.Properties properties, @Nonnull Host host) {
        super(properties);
        this.host = host;
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, INITIAL_STAGE));
    }

    @Override
    protected void createBlockStateDefinition(@Nonnull StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    /**
     * The expected recharge time emerges from the chance combined with the random tick speed.
     * <p>
     * One successful roll buys one stage, so a full recharge costs as many of them as there are stages.
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

        int stage = state.getValue(STAGE);
        if (stage < FINAL_STAGE) {
            BlockState advancedState = state.setValue(STAGE, stage + 1);
            level.setBlockAndUpdate(pos, advancedState);
            playRegenEffects(pos, level, advancedState);
            return;
        }

        OreRegenDataManager dataManager = SettlementsDagger.serverOrThrow().oreRegenDataManager();
        dataManager.rollForHost(this.host)
                .flatMap(dataManager::resolveBlockState)
                .ifPresent(rechargedState -> {
                    level.setBlockAndUpdate(pos, rechargedState);
                    playRegenEffects(pos, level, rechargedState);
                });
    }

    private static void playRegenEffects(@Nonnull BlockPos pos,
                                         @Nonnull ServerLevel level,
                                         @Nonnull BlockState newState) {
        Location effectLocation = Location.of(pos, level).center(true);
        ParticleRegistry.oreRecharge(effectLocation, newState);
        effectLocation.playSound(newState.getSoundType(level, pos, null).getPlaceSound(),
                0.6F, 0.9F + level.getRandom().nextFloat() * 0.2F, SoundSource.BLOCKS);
    }

}
