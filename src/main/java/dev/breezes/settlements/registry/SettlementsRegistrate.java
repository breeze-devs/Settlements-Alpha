package dev.breezes.settlements.registry;

import com.tterrag.registrate.AbstractRegistrate;
import dev.breezes.settlements.SettlementsMod;
import net.minecraftforge.data.event.GatherDataEvent;

public final class SettlementsRegistrate extends AbstractRegistrate<SettlementsRegistrate> {

    private SettlementsRegistrate() {
        super(SettlementsMod.MOD_ID);
    }

    public static SettlementsRegistrate create() {
        SettlementsRegistrate registrate = new SettlementsRegistrate();
        registrate.registerEventListeners(registrate.getModEventBus());
        return registrate;
    }

    @Override
    protected void onData(GatherDataEvent event) {
        super.onData(event);
    }
}
