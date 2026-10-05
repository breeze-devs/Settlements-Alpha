package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientAnimationModule;
import dev.breezes.settlements.infrastructure.rendering.animation.VillagerAnimatorFactory;

@ClientSessionScope
@Subcomponent(modules = {
        ClientAnimationModule.class,
})
public interface ClientSessionComponent {

    VillagerAnimatorFactory villagerAnimatorFactory();

    @Subcomponent.Factory
    interface Factory {
        ClientSessionComponent create();
    }

}
