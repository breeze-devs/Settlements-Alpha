package dev.breezes.settlements.di;

import dev.breezes.settlements.shared.annotations.functional.ClientSide;

/**
 * Implemented by {@link ClientScope} singletons that hold state belonging to one play session.
 */
@ClientSide
public interface ClientSessionResettable {

    void onClientSessionEnded();

}
