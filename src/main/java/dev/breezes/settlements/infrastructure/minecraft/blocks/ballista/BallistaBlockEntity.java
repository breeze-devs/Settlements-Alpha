package dev.breezes.settlements.infrastructure.minecraft.blocks.ballista;

import dev.breezes.settlements.bootstrap.registry.blockentities.BlockEntityTypeRegistry;
import dev.breezes.settlements.bootstrap.registry.items.ItemRegistry;
import dev.breezes.settlements.domain.animation.AnimationFrame;
import dev.breezes.settlements.domain.ballista.BallistaAim;
import dev.breezes.settlements.domain.ballista.BallistaAnimator;
import dev.breezes.settlements.domain.ballista.BallistaIntentPrediction;
import dev.breezes.settlements.domain.ballista.BallistaStateMachine;
import dev.breezes.settlements.domain.tags.SettlementsEntityTypeTags;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.domain.world.location.Location;
import dev.breezes.settlements.shared.annotations.functional.ClientSide;
import dev.breezes.settlements.shared.annotations.functional.ServerSide;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A placed ballista's machine state, ammunition, and aim, synchronized between server and client.
 */
public class BallistaBlockEntity extends BlockEntity {

    private static final String NBT_INTENT_YAW = "intentYaw";
    private static final String NBT_INTENT_PITCH = "intentPitch";
    private static final String NBT_YAW = "yaw";
    private static final String NBT_PITCH = "pitch";
    private static final String NBT_COCKED = "cocked";
    private static final String NBT_WINDING = "winding";
    private static final String NBT_FIRING = "firing";
    private static final String NBT_AMMUNITION = "ammunition";
    private static final String NBT_SEATER = "seater";
    private static final String NBT_SPARES_VILLAGER_ALLIES = "sparesVillagerAllies";

    private static final float DEFAULT_YAW_DEGREES = 180.0F;
    private static final float LEVEL_PITCH_DEGREES = 0.0F;

    private static final float SLEW_YAW_DEGREES_PER_TICK = 3.0F;
    private static final float SLEW_PITCH_DEGREES_PER_TICK = 1.0F;

    /**
     * An intent that has moved further than this from the last one sent is sent on the next tick.
     */
    private static final float INTENT_SYNC_THRESHOLD_DEGREES = 1.0F;

    /**
     * A smaller change is sent once this long has passed since the last send, so a slow turn reaches clients a few
     * ticks late rather than never.
     */
    private static final long INTENT_SYNC_MIN_INTERVAL_TICKS = ClockTicks.of(3).getTicks();

    /**
     * How far a player's eyes may be from the machine's center and still operate it.
     */
    private static final double OPERATING_RANGE_BLOCKS = 8.0;

    @Getter
    private BallistaAim aim;

    /**
     * On the server, its instants are the level's game time.
     * On a client, they are that client's own tick counter.
     */
    private BallistaStateMachine machineState;

    /**
     * The ammunition loaded in the machine, a single item, or empty.
     */
    private ItemStack ammunition;

    /**
     * The entity that seated the loaded ammunition, which the shot is credited to.
     */
    @ServerSide
    @Nullable
    private UUID seaterId;

    /**
     * Whether the loaded shot passes through villager allies, which it does when one of them seated it.
     */
    @ServerSide
    private boolean sparesVillagerAllies;

    /**
     * The aim's angles as of the previous tick, which a frame interpolates from.
     */
    @ClientSide
    private float previousYaw;

    @ClientSide
    private float previousPitch;

    @ClientSide
    private final BallistaIntentPrediction intentPrediction;

    /**
     * Whether this client has been given the machine's heading, not just its intent.
     */
    @ClientSide
    private boolean headingKnown;

    /**
     * Ticks this client's copy of the machine has run: the counter a client keeps its machine state and animation on.
     */
    @ClientSide
    private long clientTicks;

    /**
     * The stage the server last reported to this client.
     */
    @ClientSide
    private BallistaStateMachine.Stage syncedStage;

    @ClientSide
    private final BallistaAnimator animator;

    /**
     * The aim whose intent clients were last sent, when that was, and whether the intent has changed since.
     */
    @ServerSide
    private BallistaAim sentAim;

    @ServerSide
    private long intentLastSentGameTime;

    @ServerSide
    private boolean intentUnsent;

    public BallistaBlockEntity(@Nonnull BlockPos pos,
                               @Nonnull BlockState blockState) {
        super(BlockEntityTypeRegistry.BALLISTA.get(), pos, blockState);
        this.aim = BallistaAim.facing(DEFAULT_YAW_DEGREES, LEVEL_PITCH_DEGREES);
        this.machineState = BallistaStateMachine.unwound();
        this.ammunition = ItemStack.EMPTY;
        this.seaterId = null;
        this.sparesVillagerAllies = false;
        this.previousYaw = this.aim.getYaw();
        this.previousPitch = this.aim.getPitch();
        this.headingKnown = false;
        this.clientTicks = 0L;
        this.syncedStage = this.machineState.getStage();
        this.animator = new BallistaAnimator();
        this.intentPrediction = new BallistaIntentPrediction();
        this.sentAim = this.aim;
        this.intentLastSentGameTime = 0L;
        this.intentUnsent = false;
    }

    /**
     * Entrypoint for player interactions.
     */
    public ItemInteractionResult use(@Nonnull Player player, @Nonnull InteractionHand hand) {
        boolean serverSide = this.level != null && !this.level.isClientSide();
        long now = serverSide ? this.level.getGameTime() : this.clientTicks;

        if (serverSide) {
            // A machine that went unticked past its release tick would otherwise settle out of the firing below with
            // the shot still in it, and the shot would never leave
            this.releaseShotIfDue(now);
        }

        BallistaStateMachine currentState = this.machineState.settledAt(now);
        // Read from the main hand on either pass, since the off hand's pass carries the off hand's stack, which reads
        // as an empty hand whenever that hand holds nothing
        ItemStack mainHand = player.getMainHandItem();
        BallistaVerb verb = BallistaVerb.resolve(BallistaVerb.Situation.builder()
                .mainHand(mainHandOf(mainHand))
                .sneaking(player.isShiftKeyDown())
                .stage(currentState.getStage())
                .loaded(!this.ammunition.isEmpty())
                .build());

        if (verb == BallistaVerb.DEFER_TO_VANILLA) {
            // On the off hand's pass too, so its item still gets the turn vanilla would give it
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            // Every use the machine answers is answered on the main hand's pass
            return ItemInteractionResult.CONSUME;
        }
        if (!serverSide) {
            // Only the server acts on the machine; claiming the use keeps the off hand out of it
            return ItemInteractionResult.SUCCESS;
        }

        return switch (verb) {
            case WIND -> this.startWinding(currentState, now);
            case LOAD_AMMO -> this.loadAmmunition(mainHand, player, now);
            case UNLOAD_AMMO -> this.unloadAmmunition(player, now);
            // The client holds aim mode, so entering it leaves the machine as it is
            case ENTER_AIM -> ItemInteractionResult.CONSUME;
            case REFUSE -> this.refuse();
            default -> throw new IllegalStateException("Unexpected value: " + verb);
        };
    }

    private static BallistaVerb.MainHand mainHandOf(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return BallistaVerb.MainHand.EMPTY;
        }

        return stack.is(ItemRegistry.BALLISTA_BOLT.get()) ? BallistaVerb.MainHand.AMMUNITION : BallistaVerb.MainHand.OTHER;
    }

    @ServerSide
    private ItemInteractionResult startWinding(@Nonnull BallistaStateMachine unwound, long gameTime) {
        this.machineState = unwound.startWindingAt(gameTime);
        this.sendUpdate(gameTime);

        BallistaSoundPalette.playBeat(BallistaStateMachine.Beat.STROKE_BEGINS, this.soundLocation());
        return ItemInteractionResult.CONSUME;
    }

    @ServerSide
    private ItemInteractionResult loadAmmunition(@Nonnull ItemStack held, @Nonnull Player player, long gameTime) {
        ItemStack ammunition = held.copyWithCount(1);
        ammunition.remove(DataComponents.INTANGIBLE_PROJECTILE);
        if (!player.hasInfiniteMaterials()) {
            held.shrink(1);
        }

        this.ammunition = ammunition;
        this.seaterId = player.getUUID();
        // Decided here rather than from the shot's owner, which is missing whenever the seater is not loaded at release
        this.sparesVillagerAllies = player.getType().is(SettlementsEntityTypeTags.VILLAGER_ALLIES);
        this.setChanged();
        this.sendUpdate(gameTime);

        BallistaSoundPalette.loadBolt(this.soundLocation());
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Takes the seated bolt back out, leaving the machine cocked.
     */
    @ServerSide
    private ItemInteractionResult unloadAmmunition(@Nonnull Player player, long gameTime) {
        ItemStack unloaded = this.seatedAmmunitionCopy();
        this.ammunition = ItemStack.EMPTY;
        this.seaterId = null;
        this.sparesVillagerAllies = false;
        this.setChanged();
        this.sendUpdate(gameTime);

        if (!player.hasInfiniteMaterials()) {
            ItemHandlerHelper.giveItemToPlayer(player, unloaded);
        }
        BallistaSoundPalette.unloadBolt(this.soundLocation());
        return ItemInteractionResult.CONSUME;
    }

    @ServerSide
    private ItemInteractionResult refuse() {
        BallistaSoundPalette.refuseInteraction(this.soundLocation());
        return ItemInteractionResult.CONSUME;
    }

    /**
     * Answers a fresh rising edge of redstone input.
     */
    @ServerSide
    public void receiveRedstonePulse() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }

        long gameTime = this.level.getGameTime();

        this.releaseShotIfDue(gameTime);

        BallistaStateMachine currentState = this.machineState.settledAt(gameTime);
        switch (currentState.getStage()) {
            case UNWOUND -> this.startWinding(currentState, gameTime);
            case COCKED -> {
                if (this.ammunition.isEmpty()) {
                    BallistaSoundPalette.refuseInteraction(this.soundLocation());
                    return;
                }
                this.machineState = currentState.fireAt(gameTime);
                this.setChanged();
                this.sendUpdate(gameTime);
            }
            case WINDING, FIRING -> BallistaSoundPalette.refuseInteraction(this.soundLocation());
        }
    }

    /**
     * Tells the machine where to point. It turns there at its slew rate, and clients are sent the new intent.
     */
    @ServerSide
    public void aimAt(float yaw, float pitch) {
        BallistaAim aimed = this.aim.withIntent(yaw, pitch);
        if (aimed == this.aim) {
            return;
        }

        this.aim = aimed;
        this.intentUnsent = true;
        this.setChanged();
    }

    /**
     * Turns this client's machine toward an adjustment its own player just made, ahead of the server's answer.
     */
    @ClientSide
    void predictIntent(float yaw, float pitch) {
        this.aim = this.aim.withIntent(yaw, pitch);
        this.intentPrediction.predictedAt(this.clientTicks);
    }

    public static boolean isOutsideOperatingRange(@Nonnull Vec3 eyePosition, @Nonnull BlockPos pos) {
        return !(eyePosition.distanceToSqr(Vec3.atCenterOf(pos)) <= OPERATING_RANGE_BLOCKS * OPERATING_RANGE_BLOCKS);
    }

    /**
     * Readies a newly placed ballista. It always starts unwound and empty, whatever state its item carried.
     */
    public void prepareForPlacement(@Nullable LivingEntity placer) {
        this.machineState = BallistaStateMachine.unwound();
        this.ammunition = ItemStack.EMPTY;
        this.seaterId = null;
        this.sparesVillagerAllies = false;
        if (placer != null) {
            this.aim = BallistaAim.forPlacement(placer.getYRot());
            this.snapInterpolation();
            this.headingKnown = true;
        }
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.sendUpdate(this.level.getGameTime());
        }
    }

    /**
     * A copy of the seated ammunition. Empty when none is seated.
     */
    @ServerSide
    public ItemStack seatedAmmunitionCopy() {
        return this.ammunition.copy();
    }

    @ServerSide
    public static void serverTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull BallistaBlockEntity ballista) {
        // Ignore idle ballistas for performance
        if (ballista.aim.isAtRest() && !ballista.intentUnsent && !ballista.machineState.isTransient()) {
            return;
        }

        long gameTime = level.getGameTime();

        ballista.aim = ballista.aim.step(SLEW_YAW_DEGREES_PER_TICK, SLEW_PITCH_DEGREES_PER_TICK);

        if (ballista.machineState.isTransient()) {
            ballista.tickMachineState(gameTime);
        }
        if (ballista.intentUnsent && ballista.isIntentSyncDue(gameTime)) {
            ballista.sendUpdate(gameTime);
        }
    }

    @ClientSide
    public static void clientTick(@Nonnull Level level,
                                  @Nonnull BlockPos pos,
                                  @Nonnull BlockState state,
                                  @Nonnull BallistaBlockEntity ballista) {
        ballista.clientTicks++;

        ballista.previousYaw = ballista.aim.getYaw();
        ballista.previousPitch = ballista.aim.getPitch();
        ballista.aim = ballista.intentPrediction.settle(ballista.aim, ballista.clientTicks);
        if (!ballista.aim.isAtRest()) {
            ballista.aim = ballista.aim.step(SLEW_YAW_DEGREES_PER_TICK, SLEW_PITCH_DEGREES_PER_TICK);
            // Only the step that arrives sounds, so an aim set outright (load, first packet, placement) stays silent
            if (ballista.aim.isAtRest()) {
                BallistaSoundPalette.finishedTurning(ballista.soundLocation());
            }
        }

        // Clear the displayed bolt before settling a completed firing loses its release timing
        if (ballista.machineState.hasReleasedAt(ballista.clientTicks)) {
            ballista.ammunition = ItemStack.EMPTY;
        }
        ballista.machineState = ballista.machineState.settledAt(ballista.clientTicks);
        ballista.animator.updateAnimationFromState(ballista.machineState);
    }

    /**
     * The render aim, with partialTick selecting between the previous (0) and current (1) client tick.
     */
    @ClientSide
    BallistaAim interpolatedAim(float partialTick) {
        return BallistaAim.facing(Mth.rotLerp(partialTick, this.previousYaw, this.aim.getYaw()),
                Mth.lerp(partialTick, this.previousPitch, this.aim.getPitch()));
    }

    /**
     * What a HUD should render given the main-hand item.
     */
    @ClientSide
    public BallistaHudState hudStateForHeldItem(@Nonnull ItemStack mainHand) {
        // The server's stage rather than the one drawn, which a client that joined mid-wind holds unwound until the
        // wind ends; a use is answered by the server's
        return BallistaHudState.resolve(this.syncedStage, !this.ammunition.isEmpty(), mainHandOf(mainHand));
    }

    /**
     * The rig's clip-driven pose for a frame drawn the given fraction of a tick after this client's latest tick.
     */
    @ClientSide
    AnimationFrame sampleAnimation(float partialTick) {
        return this.animator.sample(this.machineState, this.clientTicks, partialTick);
    }

    /**
     * Whether this client draws a bolt on the socket.
     */
    @ClientSide
    boolean drawsBoltOnSocket() {
        return !this.ammunition.isEmpty();
    }

    /**
     * Tick the current ballista state.
     */
    @ServerSide
    private void tickMachineState(long gameTime) {
        this.machineState.beatAt(gameTime).ifPresent(beat -> BallistaSoundPalette.playBeat(beat, this.soundLocation()));
        this.releaseShotIfDue(gameTime);

        BallistaStateMachine settled = this.machineState.settledAt(gameTime);
        if (settled == this.machineState) {
            return;
        }

        this.machineState = settled;
        this.setChanged();
        this.sendUpdate(gameTime);
    }

    /**
     * Launches and clears the committed shot once server time reaches its release tick.
     * Credits the entity that loaded it if that entity is still in the level.
     */
    @ServerSide
    private void releaseShotIfDue(long gameTime) {
        if (this.ammunition.isEmpty() || !this.machineState.hasReleasedAt(gameTime)
                || !(this.level instanceof ServerLevel serverLevel)) {
            return;
        }

        Entity seater = this.seaterId == null ? null : serverLevel.getEntity(this.seaterId);
        BallistaLauncher.launch(serverLevel, this.worldPosition, this.aim, this.ammunition, seater,
                this.sparesVillagerAllies);

        this.ammunition = ItemStack.EMPTY;
        this.seaterId = null;
        this.sparesVillagerAllies = false;

        this.setChanged();
        this.sendUpdate(gameTime);
    }

    private Location soundLocation() {
        return Location.of(this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5, this.level);
    }

    @ServerSide
    private boolean isIntentSyncDue(long gameTime) {
        return this.aim.intentDeviationFrom(this.sentAim) > INTENT_SYNC_THRESHOLD_DEGREES
                || gameTime - this.intentLastSentGameTime >= INTENT_SYNC_MIN_INTERVAL_TICKS;
    }

    @ServerSide
    private void sendUpdate(long gameTime) {
        this.sentAim = this.aim;
        this.intentLastSentGameTime = gameTime;
        this.intentUnsent = false;
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    /**
     * A heading set outright, rather than reached by turning, starts the interpolation over from it; otherwise the
     * next frame draws a swing from wherever the machine pointed before.
     */
    private void snapInterpolation() {
        this.previousYaw = this.aim.getYaw();
        this.previousPitch = this.aim.getPitch();
    }

    @Override
    protected void saveAdditional(@Nonnull CompoundTag tag,
                                  @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        this.writeAim(tag);

        if (this.machineState.getStage() == BallistaStateMachine.Stage.COCKED) {
            tag.putBoolean(NBT_COCKED, true);
        }
        this.writeAmmunition(tag, registries);
        // Kept out of writeAmmunition, whose tag clients are sent too
        if (this.seaterId != null) {
            tag.putUUID(NBT_SEATER, this.seaterId);
        }
        if (this.sparesVillagerAllies) {
            tag.putBoolean(NBT_SPARES_VILLAGER_ALLIES, true);
        }
    }

    @Override
    protected void loadAdditional(@Nonnull CompoundTag tag,
                                  @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.aim = readAim(tag);
        this.snapInterpolation();
        this.sentAim = this.aim;
        this.intentUnsent = false;
        this.machineState = tag.getBoolean(NBT_COCKED) ? BallistaStateMachine.cocked() : BallistaStateMachine.unwound();
        this.ammunition = readAmmunition(tag, registries);
        this.seaterId = tag.hasUUID(NBT_SEATER) ? tag.getUUID(NBT_SEATER) : null;
        this.sparesVillagerAllies = tag.getBoolean(NBT_SPARES_VILLAGER_ALLIES);
    }

    private void writeAim(@Nonnull CompoundTag tag) {
        tag.putFloat(NBT_INTENT_YAW, this.aim.getIntentYaw());
        tag.putFloat(NBT_INTENT_PITCH, this.aim.getIntentPitch());
    }

    /**
     * Restores the aim directly at the saved target angles.
     * Missing yaw uses the default heading; missing pitch is level.
     */
    private static BallistaAim readAim(@Nonnull CompoundTag tag) {
        float yaw = tag.contains(NBT_INTENT_YAW, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(NBT_INTENT_YAW)
                : DEFAULT_YAW_DEGREES;
        float pitch = tag.contains(NBT_INTENT_PITCH, Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat(NBT_INTENT_PITCH)
                : LEVEL_PITCH_DEGREES;
        return BallistaAim.facing(yaw, pitch);
    }

    private void writeAmmunition(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        if (!this.ammunition.isEmpty()) {
            tag.put(NBT_AMMUNITION, this.ammunition.save(registries));
        }
    }

    private static ItemStack readAmmunition(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        if (!tag.contains(NBT_AMMUNITION, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        return ItemStack.parse(registries, tag.getCompound(NBT_AMMUNITION)).orElse(ItemStack.EMPTY);
    }

    /**
     * The initial client snapshot, including the current angles of a turn in progress.
     * See {@link #handleUpdateTag}
     */
    @Override
    @ServerSide
    public CompoundTag getUpdateTag(@Nonnull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.writeAim(tag);
        // New observers need the current angles to follow a turn already in progress
        tag.putFloat(NBT_YAW, this.aim.getYaw());
        tag.putFloat(NBT_PITCH, this.aim.getPitch());
        this.writeSyncedStage(tag);
        this.writeAmmunition(tag, registries);
        return tag;
    }

    /**
     * See {@link #getUpdateTag}
     */
    @Override
    @ClientSide
    public void handleUpdateTag(@Nonnull CompoundTag tag,
                                @Nonnull HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        float yaw = tag.contains(NBT_YAW, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(NBT_YAW) : this.aim.getIntentYaw();
        float pitch = tag.contains(NBT_PITCH, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(NBT_PITCH) : this.aim.getIntentPitch();
        this.aim = BallistaAim.facing(yaw, pitch).withIntent(this.aim.getIntentYaw(), this.aim.getIntentPitch());
        this.snapInterpolation();
        this.headingKnown = true;
        this.syncedStage = readSyncedStage(tag);
        this.machineState = this.syncedStage == BallistaStateMachine.Stage.COCKED
                ? BallistaStateMachine.cocked()
                : BallistaStateMachine.unwound();
        if (this.syncedStage == BallistaStateMachine.Stage.FIRING) {
            // An initial snapshot does not replay firing, so no local release will clear this bolt.
            this.ammunition = ItemStack.EMPTY;
        }
    }

    /**
     * Sent to the client as a synchronization packet.
     * See {@link #onDataPacket}.
     */
    @Override
    @ServerSide
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this, (blockEntity, registries) -> {
            CompoundTag tag = new CompoundTag();
            this.writeAim(tag);
            this.writeSyncedStage(tag);
            this.writeAmmunition(tag, registries);
            return tag;
        });
    }

    /**
     * Received from the server as a synchronization packet.
     * See {@link #getUpdatePacket()}.
     */
    @Override
    @ClientSide
    public void onDataPacket(@Nonnull Connection connection,
                             @Nonnull ClientboundBlockEntityDataPacket packet,
                             @Nonnull HolderLookup.Provider lookupProvider) {
        CompoundTag tag = packet.getTag();

        // Sync the stage
        BallistaStateMachine.Stage stage = readSyncedStage(tag);
        if (stage != this.syncedStage) {
            this.syncedStage = stage;
            // The server can finish before the local release; keep that firing's clock until it settles locally.
            boolean finishingLocalShot = stage == BallistaStateMachine.Stage.UNWOUND
                    && this.machineState.getStage() == BallistaStateMachine.Stage.FIRING;
            if (!finishingLocalShot) {
                this.machineState = switch (stage) {
                    case UNWOUND -> BallistaStateMachine.unwound();
                    case WINDING -> BallistaStateMachine.unwound().startWindingAt(this.clientTicks);
                    case COCKED -> BallistaStateMachine.cocked();
                    case FIRING -> BallistaStateMachine.cocked().fireAt(this.clientTicks);
                };
            }

            this.animator.updateAnimationFromState(this.machineState);
        }

        // Every packet, stage change or not, since unloading empties the socket without one
        this.updateAmmunitionFromServer(readAmmunition(tag, lookupProvider));

        // Sync aim
        if (tag.contains(NBT_INTENT_YAW, Tag.TAG_ANY_NUMERIC)
                && tag.contains(NBT_INTENT_PITCH, Tag.TAG_ANY_NUMERIC)) {
            float intentYaw = tag.getFloat(NBT_INTENT_YAW);
            float intentPitch = tag.getFloat(NBT_INTENT_PITCH);

            if (this.headingKnown) {
                this.aim = this.intentPrediction.receive(this.aim, intentYaw, intentPitch, this.clientTicks);
                return;
            }

            this.aim = BallistaAim.facing(intentYaw, intentPitch);
            this.snapInterpolation();
            this.headingKnown = true;
        }
    }

    @ClientSide
    private void updateAmmunitionFromServer(@Nonnull ItemStack received) {
        if (this.syncedStage != BallistaStateMachine.Stage.FIRING
                && this.machineState.getStage() != BallistaStateMachine.Stage.FIRING) {
            this.ammunition = received;
            return;
        }

        // Firing snapshots can lead or trail the local release: an empty one must not hide the bolt early, and a
        // loaded one must not bring it back after release, even when the local firing has already settled.
        if (!this.machineState.holdsShotAt(this.clientTicks)) {
            this.ammunition = ItemStack.EMPTY;
        } else if (!received.isEmpty()) {
            this.ammunition = received;
        }
    }

    /**
     * Writes the machine's stage as of the current game time.
     */
    @ServerSide
    private void writeSyncedStage(@Nonnull CompoundTag tag) {
        // Settled first, since a machine that has not ticked since its clip ran out still holds that wind or firing
        BallistaStateMachine current = this.level == null
                ? this.machineState
                : this.machineState.settledAt(this.level.getGameTime());
        switch (current.getStage()) {
            case UNWOUND -> {
            }
            case WINDING -> tag.putBoolean(NBT_WINDING, true);
            case COCKED -> tag.putBoolean(NBT_COCKED, true);
            case FIRING -> tag.putBoolean(NBT_FIRING, true);
        }
    }

    @ClientSide
    private static BallistaStateMachine.Stage readSyncedStage(@Nonnull CompoundTag tag) {
        if (tag.getBoolean(NBT_WINDING)) {
            return BallistaStateMachine.Stage.WINDING;
        }
        if (tag.getBoolean(NBT_FIRING)) {
            return BallistaStateMachine.Stage.FIRING;
        }

        return tag.getBoolean(NBT_COCKED) ? BallistaStateMachine.Stage.COCKED : BallistaStateMachine.Stage.UNWOUND;
    }

}
