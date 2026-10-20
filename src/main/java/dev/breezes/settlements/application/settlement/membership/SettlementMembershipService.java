package dev.breezes.settlements.application.settlement.membership;

import dev.breezes.settlements.application.ai.brain.VanillaAmbientBehaviorPackages;
import dev.breezes.settlements.application.settlement.persistence.SettlementStore;
import dev.breezes.settlements.application.settlement.query.SettlementQueryService;
import dev.breezes.settlements.bootstrap.registry.attachments.AttachmentRegistry;
import dev.breezes.settlements.di.ServerScope;
import dev.breezes.settlements.domain.settlement.model.Settlement;
import dev.breezes.settlements.domain.settlement.model.SettlementGatherings;
import dev.breezes.settlements.domain.settlement.model.SettlementLog;
import dev.breezes.settlements.domain.settlement.model.SettlementLogEntry;
import dev.breezes.settlements.domain.settlement.model.SettlementLogType;
import dev.breezes.settlements.domain.settlement.model.SettlementRoster;
import dev.breezes.settlements.domain.settlement.model.SettlementRosterEntry;
import dev.breezes.settlements.domain.world.CivilDateTime;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import jakarta.inject.Inject;
import lombok.AccessLevel;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Manages villagers' settlement membership and gathering attendance.
 * Roster changes and their associated log entries are persisted together in the settlement's dimension.
 * <p>
 * A membership resolves only while its dimension is available and its settlement is stored.
 * Must be called on the server thread, with villagers in a server level.
 */
@ServerSide
@ServerScope
@CustomLog
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class SettlementMembershipService {

    /**
     * Maximum check-in distance from a member to its meeting bell, measured as the Manhattan distance between their
     * block positions, in blocks.
     */
    public static final int CHECK_IN_DISTANCE = VanillaAmbientBehaviorPackages.MEET_WALK_CLOSE_ENOUGH_DISTANCE;

    private static final Set<SettlementLogType> CONVERSION_LOG_TYPES =
            Set.of(SettlementLogType.ZOMBIFIED, SettlementLogType.BECAME_WITCH, SettlementLogType.LEFT);

    private final SettlementStore settlementStore;
    private final SettlementQueryService settlementQueryService;

    /**
     * Assigns the villagers to the settlement, refreshing their roster entries and logging JOINED in input order.
     * Replaces previous memberships and removes entries from other settlements in loaded dimensions without logging
     * departures.
     *
     * @param level     the level storing the settlement
     * @param villagers the villagers joining, which may stand in another level than the settlement's
     * @return the updated settlement, or empty if it does not exist; an absent settlement leaves all state unchanged
     */
    public Optional<Settlement> join(@Nonnull ServerLevel level,
                                     @Nonnull UUID settlementId,
                                     @Nonnull List<BaseVillager> villagers) {
        return this.admit(level, settlementId, villagers, SettlementLogType.JOINED, null);
    }

    /**
     * Assigns a founding's members to the settlement they founded, as {@link #join} does, logging each JOINED entry
     * with the detail {@link SettlementLogType#FOUNDER_DETAIL}.
     *
     * @param level    the level storing the settlement
     * @param founders the founding members, in the order to log them
     * @return the updated settlement, or empty if it does not exist; an absent settlement leaves all state unchanged
     */
    public Optional<Settlement> joinFounders(@Nonnull ServerLevel level,
                                             @Nonnull UUID settlementId,
                                             @Nonnull List<BaseVillager> founders) {
        return this.admit(level, settlementId, founders, SettlementLogType.JOINED, SettlementLogType.FOUNDER_DETAIL);
    }

    /**
     * Assigns a newborn to a parent's settlement and logs BORN.
     * Among resolvable parental memberships, prefers the settlement claiming the birthplace, then the initiator's,
     * then the partner's. Leaves the child unchanged if neither parent's membership resolves.
     *
     * @param child     the newborn; assign its home first so the roster records its housing
     * @param initiator the parent who initiated the courtship
     * @param partner   the other parent
     */
    public void inheritAtBirth(@Nonnull BaseVillager child,
                               @Nonnull BaseVillager initiator,
                               @Nonnull BaseVillager partner) {
        ServerLevel level = serverLevelOf(child);
        MinecraftServer server = level.getServer();
        List<SettlementMembership> parentMemberships = Stream.of(initiator, partner)
                .flatMap(parent -> parent.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP).stream())
                .filter(membership -> this.resolve(server, membership).isPresent())
                .toList();
        Optional<SettlementMembership> birthplace = this.settlementQueryService
                .getSettlementAt(level, child.blockPosition())
                .map(settlement -> new SettlementMembership(settlement.id(), level.dimension()));

        birthplace.filter(parentMemberships::contains)
                .or(() -> parentMemberships.stream().findFirst())
                .flatMap(membership -> this.resolve(server, membership))
                .ifPresent(resolved -> this.admit(resolved.level(), resolved.settlement().id(), List.of(child),
                        SettlementLogType.BORN, null));
    }

    /**
     * Restores a missing roster entry from the villager's current state, or clears a membership that cannot be resolved.
     * An existing entry in the resolved settlement is left unchanged. Neither action is logged.
     */
    public void repair(@Nonnull BaseVillager villager) {
        Optional<SettlementMembership> membership = villager.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP);
        if (membership.isEmpty()) {
            return;
        }

        Optional<Resolved> resolved = this.resolve(serverLevelOf(villager).getServer(), membership.get());
        if (resolved.isEmpty()) {
            this.unassign(villager, membership.get());
            return;
        }

        if (resolved.get().settlement().roster().find(villager.getUUID()).isEmpty()) {
            log.debug("Restoring the missing roster row of villager {} in settlement {}",
                    villager.getUUID(), membership.get().settlementId());
            this.admit(resolved.get().level(), membership.get().settlementId(), List.of(villager), null, null);
        }
    }

    /**
     * Clears a membership that cannot be resolved, leaving the villager unassigned without logging the change.
     */
    public void clearUnresolvableMembership(@Nonnull BaseVillager villager) {
        Optional<SettlementMembership> membership = villager.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP);
        if (membership.isPresent() && this.resolve(serverLevelOf(villager).getServer(), membership.get()).isEmpty()) {
            this.unassign(villager, membership.get());
        }
    }

    /**
     * Removes a member's roster entry for permanent removal, preserving its membership attachment.
     * Killed villagers are logged as DIED; discarded villagers are not logged.
     *
     * @param reason why the villager is removed
     * @throws IllegalArgumentException if the reason does not destroy the villager
     */
    public void recordRemoval(@Nonnull BaseVillager villager, @Nonnull Entity.RemovalReason reason) {
        Validate.isTrue(reason.shouldDestroy(), "Villager %s is not removed for good: %s", villager.getUUID(), reason);
        // Discarding can be part of a conversion, so it must not produce a death entry
        this.drop(villager, reason == Entity.RemovalReason.KILLED ? SettlementLogType.DIED : null);
    }

    /**
     * Records that a member was converted into an entity that is not a living member: its row leaves its settlement's
     * roster, if still listed, and the conversion is logged once as the given type. The villager keeps its membership
     * attachment. Nothing happens for a villager that belongs to no existing settlement.
     *
     * @param type the conversion's log type: ZOMBIFIED, BECAME_WITCH, or LEFT for a conversion into a vanilla villager
     * @throws IllegalArgumentException if the type is not one of those
     */
    public void recordConversion(@Nonnull BaseVillager villager, @Nonnull SettlementLogType type) {
        Validate.isTrue(CONVERSION_LOG_TYPES.contains(type), "Villager %s cannot be logged as converted with %s",
                villager.getUUID(), type);
        this.drop(villager, type);
    }

    /**
     * Assigns the cured villager to the zombie's settlement, refreshing its roster entry and logging CURED.
     * Does nothing if the zombie has no resolvable membership.
     *
     * @param zombie the zombie villager it was cured from
     */
    public void recordCure(@Nonnull BaseVillager villager, @Nonnull Entity zombie) {
        MinecraftServer server = serverLevelOf(villager).getServer();
        zombie.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)
                .flatMap(membership -> this.resolve(server, membership))
                .ifPresent(resolved -> this.admit(resolved.level(), resolved.settlement().id(), List.of(villager),
                        SettlementLogType.CURED, null));
    }

    /**
     * Restores a converted villager to its preserved settlement, refreshing its roster entry and logging JOINED.
     * Does nothing if the villager has no resolvable membership.
     */
    public void recordReturn(@Nonnull BaseVillager villager) {
        MinecraftServer server = serverLevelOf(villager).getServer();
        villager.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)
                .flatMap(membership -> this.resolve(server, membership))
                .ifPresent(resolved -> this.admit(resolved.level(), resolved.settlement().id(), List.of(villager),
                        SettlementLogType.JOINED, null));
    }

    /**
     * Refreshes or restores a member's roster entry after growing up, with its gathering mark at the current count.
     * Does not log an event or write unchanged state. Does nothing if the membership cannot be resolved.
     */
    public void recordGrowingUp(@Nonnull BaseVillager villager) {
        // The mark moves to the current count because a baby never checks in: left at its birth count, a member
        // would read as having sat out every gathering of its childhood
        villager.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)
                .flatMap(membership -> this.resolve(serverLevelOf(villager).getServer(), membership))
                .ifPresent(resolved -> this.refreshRow(resolved.level(), resolved.settlement(), villager,
                        UnaryOperator.identity()));
    }

    /**
     * Records gathering attendance for an adult member with MEET active.
     * <p>
     * The member and its remembered meeting bell must be in the settlement's dimension, within
     * {@link #CHECK_IN_DISTANCE} of each other, and the bell must be on land claimed by that settlement.
     * <p>
     * Consecutive check-ins on the same calendar day share one gathering; a different day counts another, even after
     * time moves backward. The member's roster entry is refreshed or restored with its gathering mark at the resulting
     * count. Negative calendar days are ignored. No event is logged, and unchanged state is not written.
     */
    public void checkIn(@Nonnull BaseVillager villager) {
        // Check local eligibility first to avoid unnecessary settlement-store lookups
        if (villager.isBaby()
                || !villager.getBrain().isActive(Activity.MEET)
                || !villager.hasData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)) {
            return;
        }

        ServerLevel level = serverLevelOf(villager);
        SettlementMembership membership = villager.getData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP);
        Optional<GlobalPos> bell = villager.getBrain().getMemory(MemoryModuleType.MEETING_POINT);
        // A bell remembered in another dimension says nothing about where the villager stands
        if (bell.isEmpty()
                || !bell.get().dimension().equals(level.dimension())
                || !bell.get().dimension().equals(membership.dimension())
                || bell.get().pos().distManhattan(villager.blockPosition()) > CHECK_IN_DISTANCE) {
            return;
        }

        long calendarDay = new CivilDateTime(level.getDayTime()).calendarDay();
        // World time can be set before the calendar's start, but the gathering tally requires nonnegative days
        if (calendarDay < 0) {
            return;
        }

        this.settlementQueryService.getSettlementAt(level, bell.get().pos())
                .filter(claimant -> claimant.id().equals(membership.settlementId()))
                .ifPresent(settlement -> this.refreshRow(level, settlement, villager,
                        gatherings -> gatherings.checkIn(calendarDay)));
    }

    /**
     * Assigns the villagers to the settlement and refreshes their roster entries.
     *
     * @param logType the event to log for each villager in input order, or null to omit logging
     * @param detail  the detail of each logged entry, or null for none
     * @return the updated settlement, or empty if it does not exist; an absent settlement leaves all state unchanged
     */
    private Optional<Settlement> admit(ServerLevel level,
                                       UUID settlementId,
                                       List<BaseVillager> villagers,
                                       @Nullable SettlementLogType logType,
                                       @Nullable String detail) {
        if (this.settlementStore.find(level, settlementId).isEmpty()) {
            return Optional.empty();
        }

        // Remove conflicting entries first: membership must be unique within each level, and stale entries in other
        // dimensions would still count the villager there
        for (BaseVillager villager : villagers) {
            this.dropFromOtherRosters(villager.getUUID(), level, settlementId);
        }

        CivilDateTime date = new CivilDateTime(level.getDayTime());
        Optional<Settlement> stored = this.settlementStore.update(level, settlementId, settlement -> {
            int gatheringMark = settlement.gatherings().count();
            SettlementRoster roster = settlement.roster();
            SettlementLog settlementLog = settlement.log();
            for (BaseVillager villager : villagers) {
                roster = roster.with(row(villager, gatheringMark));
                if (logType != null) {
                    settlementLog = settlementLog.append(logEntry(date, logType, villager, detail));
                }
            }
            return revised(settlement, roster, settlementLog);
        });

        if (stored.isPresent()) {
            SettlementMembership membership = new SettlementMembership(settlementId, level.dimension());
            villagers.forEach(villager -> villager.setData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP, membership));
        }
        return stored;
    }

    /**
     * Updates the settlement's gathering tally and the member's roster entry together, restoring a missing entry.
     * The member's gathering mark uses the resulting count. Does not log an event or write unchanged state.
     */
    private void refreshRow(ServerLevel level,
                            Settlement settlement,
                            BaseVillager villager,
                            UnaryOperator<SettlementGatherings> gatheringsChange) {
        UUID memberId = villager.getUUID();
        if (settlement.roster().find(memberId).isEmpty()) {
            log.debug("Restoring the missing roster row of villager {} in settlement {}", memberId, settlement.id());
            // The store refuses a member another settlement in its level lists
            this.dropFromOtherRosters(memberId, level, settlement.id());
        }

        this.settlementStore.update(level, settlement.id(), stored -> {
            SettlementGatherings gatherings = gatheringsChange.apply(stored.gatherings());
            return revised(stored, stored.roster().with(row(villager, gatherings.count())), stored.log(), gatherings);
        });
    }

    /**
     * Drops the villager from its settlement's roster, logged with the given type, or not logged when it is null.
     */
    private void drop(BaseVillager villager, @Nullable SettlementLogType logType) {
        // The attachment stays: a conversion can discard the villager before its attachments are copied onto what it
        // becomes, and before the conversion is recorded
        Optional<Resolved> resolved = villager.getExistingData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP)
                .flatMap(membership -> this.resolve(serverLevelOf(villager).getServer(), membership));
        if (resolved.isEmpty()) {
            return;
        }

        UUID memberId = villager.getUUID();
        SettlementLogEntry entry = logType == null
                ? null
                : logEntry(new CivilDateTime(resolved.get().level().getDayTime()), logType, villager, null);
        this.settlementStore.update(resolved.get().level(), resolved.get().settlement().id(), settlement ->
                revised(settlement, settlement.roster().without(memberId),
                        entry == null ? settlement.log() : settlement.log().append(entry)));
    }

    private void dropFromOtherRosters(UUID memberId, ServerLevel keptLevel, UUID keptSettlementId) {
        for (ServerLevel level : keptLevel.getServer().getAllLevels()) {
            this.settlementStore.findListing(level, memberId)
                    .filter(listing -> level != keptLevel || !listing.id().equals(keptSettlementId))
                    .ifPresent(listing -> this.settlementStore.update(level, listing.id(), settlement ->
                            revised(settlement, settlement.roster().without(memberId), settlement.log())));
        }
    }

    private void unassign(BaseVillager villager, SettlementMembership membership) {
        villager.removeData(AttachmentRegistry.SETTLEMENT_MEMBERSHIP);
        log.debug("Villager {} no longer belongs to settlement {} in {}, which no longer exists", villager.getUUID(),
                membership.settlementId(), membership.dimension().location());
    }

    private Optional<Resolved> resolve(MinecraftServer server, SettlementMembership membership) {
        ServerLevel level = server.getLevel(membership.dimension());
        if (level == null) {
            return Optional.empty();
        }
        return this.settlementStore.find(level, membership.settlementId())
                .map(settlement -> new Resolved(level, settlement));
    }

    private static SettlementRosterEntry row(BaseVillager villager, int gatheringMark) {
        return SettlementRosterEntry.builder()
                .memberId(villager.getUUID())
                .profession(villager.getProfession())
                .adult(!villager.isBaby())
                .housed(villager.getBrain().hasMemoryValue(MemoryModuleType.HOME))
                .gatheringCountAtLastCheckIn(gatheringMark)
                .build();
    }

    private static SettlementLogEntry logEntry(CivilDateTime date,
                                               SettlementLogType type,
                                               BaseVillager villager,
                                               @Nullable String detail) {
        String name = villager.getName().getString();
        return SettlementLogEntry.builder()
                .date(date)
                .type(type)
                .subjectId(villager.getUUID())
                // A custom name set by command can be blank, which a log entry refuses
                .subjectName(name.isBlank() ? null : name)
                .detail(detail)
                .build();
    }

    private static Settlement revised(Settlement settlement, SettlementRoster roster, SettlementLog settlementLog) {
        return revised(settlement, roster, settlementLog, settlement.gatherings());
    }

    private static Settlement revised(Settlement settlement,
                                      SettlementRoster roster,
                                      SettlementLog settlementLog,
                                      SettlementGatherings gatherings) {
        // Preserve the original instance when nothing changes so the store can skip the write
        if (roster == settlement.roster() && settlementLog == settlement.log() && gatherings == settlement.gatherings()) {
            return settlement;
        }
        return settlement.toBuilder().roster(roster).log(settlementLog).gatherings(gatherings).build();
    }

    private static ServerLevel serverLevelOf(BaseVillager villager) {
        Validate.validState(villager.level() instanceof ServerLevel, "Villager %s is not in a server level",
                villager.getUUID());
        return (ServerLevel) villager.level();
    }

    /**
     * A stored settlement resolved from a membership.
     */
    private record Resolved(ServerLevel level, Settlement settlement) {
    }

}
