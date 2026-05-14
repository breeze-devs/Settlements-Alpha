package dev.breezes.settlements.di;

import dagger.Subcomponent;
import dev.breezes.settlements.di.modules.client.ClientAnimationModule;
import dev.breezes.settlements.infrastructure.rendering.animation.ClientAnimatorRegistry;

@ClientSessionScope
@Subcomponent(modules = {
        ClientAnimationModule.class,
})
public interface ClientSessionComponent {

    ClientAnimatorRegistry clientAnimatorRegistry();

    @Subcomponent.Factory
    interface Factory {
        ClientSessionComponent create();
    }

}
