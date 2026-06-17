package dev.breezes.settlements.application.ui.bubble;

import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.entities.ISettlementsVillager;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.network.features.ui.bubble.packet.ClientBoundBubbleSnapshotPacket;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.CustomLog;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ServerSide
@ServerScope
@CustomLog
public final class VillagerBubbleService {

    private static final ClockTicks DEFAULT_BEHAVIOR_TTL_CAP = ClockTicks.seconds(30);
    private static final ClockTicks DEFAULT_CHAT_TTL_CAP = ClockTicks.seconds(20);
    private static final ClockTicks DEFAULT_SYSTEM_TTL_CAP = ClockTicks.seconds(10);
    private static final ClockTicks DEFAULT_FLAVOR_TTL_CAP = ClockTicks.seconds(5);

    private final EnumMap<BubbleChannel, ChannelPolicy> policies;
    private final Comparator<BubbleEntry> renderComparator;

    @Inject
    public VillagerBubbleService() {
        this.policies = new EnumMap<>(BubbleChannel.class);
        this.policies.put(BubbleChannel.BEHAVIOR, ChannelPolicy.builder()
                .maxActive(1)
                .overflowPolicy(ChannelPolicy.OverflowPolicy.REPLACE_EXISTING)
                .renderOrder(0)
                .defaultTtlCap(DEFAULT_BEHAVIOR_TTL_CAP)
                .build());
        this.policies.put(BubbleChannel.CHAT, ChannelPolicy.builder()
                .maxActive(3)
                .overflowPolicy(ChannelPolicy.OverflowPolicy.DROP_OLDEST)
                .renderOrder(1)
                .defaultTtlCap(DEFAULT_CHAT_TTL_CAP)
                .build());
        this.policies.put(BubbleChannel.SYSTEM, ChannelPolicy.builder()
                .maxActive(2)
                .overflowPolicy(ChannelPolicy.OverflowPolicy.DROP_OLDEST)
                .renderOrder(2)
                .defaultTtlCap(DEFAULT_SYSTEM_TTL_CAP)
                .build());
        this.policies.put(BubbleChannel.FLAVOR, ChannelPolicy.builder()
                .maxActive(1)
                .overflowPolicy(ChannelPolicy.OverflowPolicy.REPLACE_EXISTING)
                .renderOrder(3)
                .defaultTtlCap(DEFAULT_FLAVOR_TTL_CAP)
                .build());

        this.renderComparator = buildRenderComparator(this.policies);
    }

    public ChannelPolicy getPolicy(BubbleChannel channel) {
        return this.policies.get(channel);
    }

    public boolean applyCommand(ISettlementsVillager villager, BubbleCommand command, long gameTime) {
        VillagerBubbleState state = villager.getBubbleState();
        boolean expiredRemoved = !state.pruneExpired(gameTime).isEmpty();

        boolean changed = switch (command) {
            case BubbleCommand.Upsert upsert ->
                    handleUpsert(state, upsert.channel(), upsert.ownerKey(), upsert.message(), gameTime);
            case BubbleCommand.Push push -> handlePush(state, push.channel(), push.message(), gameTime);
            case BubbleCommand.RemoveById remove -> state.removeById(remove.bubbleId()).isPresent();
            case BubbleCommand.RemoveByOwner remove ->
                    state.removeByOwner(remove.channel(), remove.ownerKey()).isPresent();
            case BubbleCommand.ClearChannel clear -> !state.clearChannel(clear.channel()).isEmpty();
        };

        if (changed || expiredRemoved) {
            publishSnapshot(villager);
        }
        return changed || expiredRemoved;
    }

    public List<BubbleEntry> getOrderedEntries(ISettlementsVillager villager) {
        List<BubbleEntry> entries = new ArrayList<>(villager.getBubbleState().getAllEntries());
        entries.sort(this.renderComparator);
        return List.copyOf(entries);
    }

    public boolean tick(ISettlementsVillager villager, long gameTime) {
        List<BubbleEntry> removedEntries = villager.getBubbleState().pruneExpired(gameTime);
        if (removedEntries.isEmpty()) {
            return false;
        }

        publishSnapshot(villager);
        return true;
    }

    private boolean handleUpsert(VillagerBubbleState state,
                                 BubbleChannel channel,
                                 String ownerKey,
                                 BubbleMessage message,
                                 long gameTime) {
        Optional<BubbleEntry> existing = state.getByOwner(channel, ownerKey);
        if (existing.isPresent()) {
            ClockTicks ttl = clampTtl(channel, message.getTtl());
            BubbleEntry updated = existing.get().withMessage(
                    message,
                    gameTime,
                    gameTime + ttl.getTicks());
            state.put(updated);
            return true;
        }

        BubbleEntry entry = buildEntry(state, channel, ownerKey, message, gameTime);
        return insertWithPolicy(state, entry, channel);
    }

    private boolean handlePush(VillagerBubbleState state,
                               BubbleChannel channel,
                               BubbleMessage message,
                               long gameTime) {
        BubbleEntry entry = buildEntry(state, channel, null, message, gameTime);
        return insertWithPolicy(state, entry, channel);
    }

    private BubbleEntry buildEntry(VillagerBubbleState state,
                                   BubbleChannel channel,
                                   String ownerKey,
                                   BubbleMessage message,
                                   long gameTime) {
        ClockTicks ttlTicks = clampTtl(channel, message.getTtl());
        return BubbleEntry.builder()
                .bubbleId(UUID.randomUUID())
                .channel(channel)
                .ownerKey(ownerKey)
                .message(message)
                .createdGameTime(gameTime)
                .expireGameTime(gameTime + ttlTicks.getTicks())
                .sequenceNumber(state.nextSequenceNumber())
                .build();
    }

    private ClockTicks clampTtl(BubbleChannel channel, ClockTicks requestedTtl) {
        ChannelPolicy policy = this.policies.get(channel);
        return policy.clampTtl(requestedTtl);
    }

    private boolean insertWithPolicy(VillagerBubbleState state, BubbleEntry entry, BubbleChannel channel) {
        ChannelPolicy policy = this.policies.get(channel);
        if (state.getChannelSize(channel) >= policy.maxActive()) {
            boolean removed = handleOverflow(state, channel, policy);
            if (!removed && policy.overflowPolicy() == ChannelPolicy.OverflowPolicy.REJECT_NEW) {
                return false;
            }
        }

        state.put(entry);
        return true;
    }

    private boolean handleOverflow(VillagerBubbleState state, BubbleChannel channel, ChannelPolicy policy) {
        List<BubbleEntry> entries = state.getEntries(channel);
        if (entries.isEmpty()) {
            return false;
        }

        Optional<BubbleEntry> candidate = selectEvictionCandidate(entries, policy.overflowPolicy());
        return candidate.flatMap(entry -> state.removeById(entry.bubbleId())).isPresent();
    }

    private Optional<BubbleEntry> selectEvictionCandidate(List<BubbleEntry> entries,
                                                          ChannelPolicy.OverflowPolicy policy) {
        Comparator<BubbleEntry> comparator = switch (policy) {
            case REPLACE_EXISTING, DROP_OLDEST -> Comparator.comparingLong(BubbleEntry::createdGameTime)
                    .thenComparingLong(BubbleEntry::sequenceNumber);
            case DROP_LOWEST_PRIORITY -> Comparator.comparingInt((BubbleEntry entry) -> entry.message().getPriority())
                    .thenComparingLong(BubbleEntry::createdGameTime)
                    .thenComparingLong(BubbleEntry::sequenceNumber);
            case REJECT_NEW -> null;
        };

        if (comparator == null) {
            return Optional.empty();
        }

        return entries.stream().min(comparator);
    }

    private static Comparator<BubbleEntry> buildRenderComparator(EnumMap<BubbleChannel, ChannelPolicy> policies) {
        return Comparator
                .comparingInt((BubbleEntry entry) -> policies.get(entry.channel()).renderOrder())
                // Higher priority values are treated as more important
                .thenComparing((left, right) -> Integer.compare(right.message().getPriority(), left.message().getPriority()))
                .thenComparingLong(BubbleEntry::createdGameTime)
                .thenComparingLong(BubbleEntry::sequenceNumber);
    }

    private void publishSnapshot(ISettlementsVillager villager) {
        List<BubbleEntrySnapshot> snapshots = this.getOrderedEntries(villager).stream()
                .map(BubbleEntrySnapshot::fromEntry)
                .toList();

        log.info("Publishing bubble snapshot: villager={} entryCount={} sources={}",
                villager.getUUID(), snapshots.size(),
                snapshots.stream().map(BubbleEntrySnapshot::sourceType).toList());

        PacketDistributor.sendToPlayersTrackingEntity(
                villager.getMinecraftEntity(),
                new ClientBoundBubbleSnapshotPacket(villager.getNetworkingId(), snapshots));
    }

}
