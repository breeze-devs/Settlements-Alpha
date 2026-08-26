package dev.breezes.settlements.domain.ai.naming;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * Server-authoritative map from a villager's UUID to that villager's current display name.
 * <p>
 * This directory answers a villager's "UUID -> current display name" from anywhere, anytime,
 * independent of whether that villager entity happens to be loaded right now.
 * <p>
 * Only living villagers are retained. A dead villager's entry is removed, keeping this
 * directory bounded instead of growing infinitely; resolving its UUID afterwards falls back
 * to the deterministic generator, so only a player-applied rename is lost with the entry.
 * <p>
 * Absence implies "never loaded." A UUID with no entry here has never been loaded on this
 * server and therefore cannot have been renamed.
 */
public interface VillagerNameDirectory {

    /**
     * Resolves {@code uuid}'s current display name.
     *
     * @param uuid the villager UUID; every caller is expected to hold a real villager identity,
     *             not merely a UUID it hasn't checked for null yet
     */
    String resolve(@Nonnull UUID uuid);

    /**
     * Records {@code uuid}'s current display name, overwriting any previous entry.
     */
    void upsert(@Nonnull UUID uuid, @Nonnull String name);

    /**
     * Removes {@code uuid}'s entry.
     * <p>
     * Only call this once the villager is actually gone for good -- never for a chunk unload.
     */
    void remove(@Nonnull UUID uuid);

}
