package dev.breezes.settlements.di.modules.server;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoSet;
import dagger.multibindings.Multibinds;
import dev.breezes.settlements.application.ai.behavior.usecases.villager.nitwit.EggCombatOption;
import dev.breezes.settlements.application.ai.threat.CombatOption;

import java.util.Set;

/**
 * Registers {@link CombatOption} implementations as a Dagger multibinding set.
 * {@link dev.breezes.settlements.application.ai.threat.CombatOptionCatalog} sorts the set by
 * {@link CombatOption#order()}.
 */
@Module
public abstract class CombatOptionModule {

    @Multibinds
    abstract Set<CombatOption> combatOptions();

    @Binds
    @IntoSet
    abstract CombatOption eggCombatOption(EggCombatOption impl);

}
