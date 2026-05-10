package dev.breezes.settlements.di.modules.server;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;
import dev.breezes.settlements.domain.ai.catalog.BehaviorKey;
import dev.breezes.settlements.domain.ai.catalog.PoolEntry;
import dev.breezes.settlements.domain.ai.catalog.ProfessionBehaviorPool;
import dev.breezes.settlements.domain.entities.VillagerProfessionKey;

/**
 * Maps each profession to its default set of available behaviors.
 * <p>
 * This module answers "what can this profession do?" — it is a pure availability mapping.
 * <p>
 * Universal behaviors (eat, wander, rest, trade) are not listed here — they are
 * merged in automatically by {@code BehaviorPoolResolver}.
 * <p>
 * PoolEntry weights are relative routine-planning preferences. Default weight 1 means normal
 * presence; larger weights make a behavior appear more often in packed schedule windows.
 * <p>
 * To add a new profession: add one {@code @Provides @IntoSet} method returning a
 * {@link ProfessionBehaviorPool}. To give an existing profession a new behavior: add
 * the {@link BehaviorKey} constant to the relevant method below.
 */
@Module
public final class PoolModule {

    @Provides
    @IntoSet
    static ProfessionBehaviorPool armorerPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.ARMORER)
                .entry(PoolEntry.of(BehaviorKey.REPAIR_IRON_GOLEM))
                .entry(PoolEntry.of(BehaviorKey.BLAST_ORE))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool butcherPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.BUTCHER)
                .entry(PoolEntry.of(BehaviorKey.BREED_PIGS))
                .entry(PoolEntry.of(BehaviorKey.SMOKE_MEAT))
                .entry(PoolEntry.of(BehaviorKey.BUTCHER_LIVESTOCK))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool cartographerPool() {
        // Cartographers have no profession-specific behaviors yet — they rely on universals only.
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.CARTOGRAPHER)
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool clericPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.CLERIC)
                .entry(PoolEntry.of(BehaviorKey.THROW_POTIONS))
                .entry(PoolEntry.of(BehaviorKey.HARVEST_SOUL_SAND))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool farmerPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.FARMER)
                .entry(PoolEntry.of(BehaviorKey.HARVEST_SUGARCANE))
                .entry(PoolEntry.of(BehaviorKey.COLLECT_HONEY))
                .entry(PoolEntry.of(BehaviorKey.HARVEST_HONEYCOMB))
                .entry(PoolEntry.of(BehaviorKey.MILK_COW))
                .entry(PoolEntry.of(BehaviorKey.BREED_CHICKENS))
                .entry(PoolEntry.of(BehaviorKey.TAME_WOLF))
                .entry(PoolEntry.of(BehaviorKey.TAME_CAT))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool fishermanPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.FISHERMAN)
                .entry(PoolEntry.of(BehaviorKey.FISHING, 5))
                .entry(PoolEntry.of(BehaviorKey.TAME_CAT))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool fletcherPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.FLETCHER)
                .entry(PoolEntry.of(BehaviorKey.BREED_CHICKENS))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool leatherworkerPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.LEATHERWORKER)
                .entry(PoolEntry.of(BehaviorKey.BREED_COWS))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool librarianPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.LIBRARIAN)
                .entry(PoolEntry.of(BehaviorKey.ENCHANT_ITEM))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool masonPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.MASON)
                .entry(PoolEntry.of(BehaviorKey.CUT_STONE))
                .entry(PoolEntry.of(BehaviorKey.HARVEST_ORE))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool nitwitPool() {
        // Nitwits have no work behaviors — their plans are leisure and social only.
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.NITWIT)
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool nonePool() {
        // Unemployed villagers have no profession work behaviors.
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.NONE)
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool shepherdPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.SHEPHERD)
                .entry(PoolEntry.of(BehaviorKey.SHEAR_SHEEP))
                .entry(PoolEntry.of(BehaviorKey.TAME_WOLF))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool toolsmithPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.TOOLSMITH)
                .entry(PoolEntry.of(BehaviorKey.REPAIR_IRON_GOLEM))
                .build();
    }

    @Provides
    @IntoSet
    static ProfessionBehaviorPool weaponsmithPool() {
        return ProfessionBehaviorPool.builder()
                .profession(VillagerProfessionKey.WEAPONSMITH)
                .entry(PoolEntry.of(BehaviorKey.REPAIR_IRON_GOLEM))
                .build();
    }

}
