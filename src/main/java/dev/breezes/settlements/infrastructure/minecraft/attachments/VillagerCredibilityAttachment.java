package dev.breezes.settlements.infrastructure.minecraft.attachments;

import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.domain.ai.credibility.CredibilityStore;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.util.ReputationUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persistence helper for a villager's {@link CredibilityStore} (the per-observer shard held in
 * {@link ReputationUtil}).
 * <p>
 * Because {@link ReputationUtil} creates stores lazily, we persist the store's scores directly
 * from the domain object. On load, a fresh store is restored and registered into the util.
 * Follows the same pattern as {@link VillagerGeneticsAttachment}.
 */
public final class VillagerCredibilityAttachment {

    /**
     * Snapshots the villager's credibility store into the entity attachment.
     * Called from {@link BaseVillager#addAdditionalSaveData}.
     */
    public static void saveFrom(@Nonnull BaseVillager villager, @Nonnull ReputationUtil reputationUtil) {
        // Fetch or create the store for this observer — ensures we always serialize the live state.
        CredibilityStore store = reputationUtil.getOrCreateStore(villager.getUUID());
        Map<UUID, Float> snapshot = store.snapshotScores();

        List<CredibilityScoreState> states = new ArrayList<>(snapshot.size());
        for (Map.Entry<UUID, Float> kv : snapshot.entrySet()) {
            states.add(new CredibilityScoreState(kv.getKey(), kv.getValue()));
        }
        villager.setData(AttachmentRegistry.VILLAGER_CREDIBILITY, VillagerCredibilityAttachmentState.of(states));
    }

    /**
     * Restores the credibility store into {@link ReputationUtil} for this villager.
     * Called from {@link BaseVillager#load}.
     *
     * @return {@code true} if persisted data was found and loaded; {@code false} on a fresh spawn
     */
    public static boolean loadInto(@Nonnull BaseVillager villager, @Nonnull ReputationUtil reputationUtil) {
        VillagerCredibilityAttachmentState state = villager.getData(AttachmentRegistry.VILLAGER_CREDIBILITY);
        if (!state.initialized()) {
            return false;
        }

        CredibilityStore store = reputationUtil.getOrCreateStore(villager.getUUID());
        Map<UUID, Float> restored = new LinkedHashMap<>(state.scores().size());
        for (CredibilityScoreState scoreState : state.scores()) {
            restored.put(scoreState.sourceId(), scoreState.score());
        }
        store.restoreScores(restored);
        return true;
    }

}
