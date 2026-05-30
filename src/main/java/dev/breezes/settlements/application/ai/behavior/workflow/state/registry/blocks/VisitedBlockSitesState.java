package dev.breezes.settlements.application.ai.behavior.workflow.state.registry.blocks;

import dev.breezes.settlements.application.ai.behavior.workflow.state.BehaviorState;
import lombok.Getter;
import net.minecraft.core.GlobalPos;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Set;

@Getter
public class VisitedBlockSitesState implements BehaviorState {

    private Set<GlobalPos> sites;

    public VisitedBlockSitesState(@Nonnull Set<GlobalPos> sites) {
        this.sites = new HashSet<>(sites);
    }

    public static VisitedBlockSitesState empty() {
        return new VisitedBlockSitesState(Set.of());
    }

    public void addSite(@Nonnull GlobalPos site) {
        this.sites.add(site);
    }

    public boolean contains(@Nonnull GlobalPos site) {
        return this.sites.contains(site);
    }

    @Override
    public void reset() {
        this.sites = new HashSet<>();
    }

}
