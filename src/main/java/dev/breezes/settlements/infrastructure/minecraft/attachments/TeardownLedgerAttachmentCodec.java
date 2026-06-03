package dev.breezes.settlements.infrastructure.minecraft.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.breezes.settlements.application.ai.behavior.teardown.DiscardEntityObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.DropLeashObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.LedgerEntry;
import dev.breezes.settlements.application.ai.behavior.teardown.ResetBlockStateObligation;
import dev.breezes.settlements.application.ai.behavior.teardown.TeardownObligation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class TeardownLedgerAttachmentCodec {

    // -------------------------------------------------------------------------
    // Per-subtype MapCodecs (dispatch requires MapCodec, not Codec)
    // -------------------------------------------------------------------------

    private static final MapCodec<DiscardEntityObligation> DISCARD_ENTITY_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("entity_id").forGetter(DiscardEntityObligation::entityId),
                    BlockPos.CODEC.fieldOf("spawn_pos").forGetter(DiscardEntityObligation::spawnPos)
            ).apply(instance, DiscardEntityObligation::new));

    private static final MapCodec<DropLeashObligation> DROP_LEASH_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    UUIDUtil.CODEC.fieldOf("animal_id").forGetter(DropLeashObligation::animalId),
                    UUIDUtil.CODEC.fieldOf("holder_id").forGetter(DropLeashObligation::holderId),
                    BlockPos.CODEC.fieldOf("animal_pos").forGetter(DropLeashObligation::animalPos)
            ).apply(instance, DropLeashObligation::new));

    private static final MapCodec<ResetBlockStateObligation> RESET_BLOCK_STATE_CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(ResetBlockStateObligation::pos),
                    ResourceLocation.CODEC.fieldOf("expected_block").forGetter(ResetBlockStateObligation::expectedBlockId),
                    Codec.STRING.fieldOf("property_name").forGetter(ResetBlockStateObligation::propertyName),
                    Codec.STRING.fieldOf("reset_value").forGetter(ResetBlockStateObligation::resetValue)
            ).apply(instance, ResetBlockStateObligation::new));

    // -------------------------------------------------------------------------
    // Dispatched obligation codec
    // -------------------------------------------------------------------------

    /**
     * Type-discriminated codec for all {@link TeardownObligation} subtypes.
     * Encodes a {@code "type"} string field that drives dispatch on decode.
     * Adding a new subtype requires: a {@code MapCodec} for it, a new case in
     * both {@link #typeKeyOf} and {@link #mapCodecForTypeKey}.
     */
    static final Codec<TeardownObligation> OBLIGATION_CODEC = Codec.STRING.dispatch(
            "type",
            TeardownLedgerAttachmentCodec::typeKeyOf,
            TeardownLedgerAttachmentCodec::mapCodecForTypeKey);

    // -------------------------------------------------------------------------
    // LedgerEntry codec
    // -------------------------------------------------------------------------

    static final Codec<LedgerEntry> ENTRY_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    OBLIGATION_CODEC.fieldOf("obligation").forGetter(LedgerEntry::getObligation),
                    Codec.INT.optionalFieldOf("failed_attempts", 0).forGetter(LedgerEntry::getFailedAttempts)
            ).apply(instance, LedgerEntry::new));

    // -------------------------------------------------------------------------
    // State codec (top-level, registered with the attachment)
    // -------------------------------------------------------------------------

    public static final Codec<TeardownLedgerAttachmentState> STATE_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ENTRY_CODEC.listOf()
                            .optionalFieldOf("entries", List.of())
                            .forGetter(TeardownLedgerAttachmentState::entries)
            ).apply(instance, TeardownLedgerAttachmentState::new));

    private TeardownLedgerAttachmentCodec() {
    }

    // -------------------------------------------------------------------------
    // Dispatch helpers
    // -------------------------------------------------------------------------

    private static String typeKeyOf(TeardownObligation obligation) {
        if (obligation instanceof DiscardEntityObligation) {
            return "discard_entity";
        }
        if (obligation instanceof DropLeashObligation) {
            return "drop_leash";
        }
        if (obligation instanceof ResetBlockStateObligation) {
            return "reset_block_state";
        }
        throw new IllegalArgumentException("Unknown TeardownObligation type for serialization: "
                + obligation.getClass().getName()
                + " — add it to TeardownLedgerAttachmentCodec");
    }

    private static MapCodec<? extends TeardownObligation> mapCodecForTypeKey(String typeKey) {
        return switch (typeKey) {
            case "discard_entity" -> DISCARD_ENTITY_CODEC;
            case "drop_leash" -> DROP_LEASH_CODEC;
            case "reset_block_state" -> RESET_BLOCK_STATE_CODEC;
            default -> throw new IllegalArgumentException("Unknown TeardownObligation type key: '" + typeKey + "'");
        };
    }

}
