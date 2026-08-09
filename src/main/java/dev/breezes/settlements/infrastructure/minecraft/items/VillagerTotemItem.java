package dev.breezes.settlements.infrastructure.minecraft.items;

import dev.breezes.settlements.bootstrap.registry.components.DataComponentRegistry;
import dev.breezes.settlements.domain.personality.OriginType;
import dev.breezes.settlements.domain.time.ClockTicks;
import dev.breezes.settlements.infrastructure.minecraft.attachments.TotemTargetAttachment;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.BaseVillager;
import dev.breezes.settlements.infrastructure.minecraft.entities.villager.conversion.VillagerConversionUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public class VillagerTotemItem extends Item {

    private static final String TOOLTIP_MODE_KEY = "item.settlements.villager_totem.tooltip.mode";
    private static final String TOOLTIP_USE_KEY = "item.settlements.villager_totem.tooltip.use";
    private static final String TOOLTIP_ITEMS_LOST_KEY = "item.settlements.villager_totem.tooltip.items_lost";
    private static final String TOOLTIP_CYCLE_KEY = "item.settlements.villager_totem.tooltip.cycle";
    private static final String ALREADY_TARGET_TYPE_KEY = "item.settlements.villager_totem.already_target_type";

    private static final double MAX_DISTANCE_SQUARED = 16.0;

    private static final ClockTicks COOLDOWN = ClockTicks.seconds(1.0);

    public VillagerTotemItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(@Nonnull ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(@Nonnull ItemStack stack) {
        return stack.copy();
    }

    public static TotemMode getMode(ItemStack stack) {
        return TotemMode.fromSerializedId(stack.getOrDefault(DataComponentRegistry.VILLAGER_TOTEM_MODE.get(),
                TotemMode.defaultMode().getSerializedId()));
    }

    public static void setMode(ItemStack stack, TotemMode mode) {
        stack.set(DataComponentRegistry.VILLAGER_TOTEM_MODE.get(), mode.getSerializedId());
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag tooltipFlag) {
        TotemMode mode = getMode(stack);
        tooltipComponents.add(Component.translatable(TOOLTIP_MODE_KEY,
                Component.translatable(mode.getTranslationKey())).withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable(TOOLTIP_CYCLE_KEY).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(TOOLTIP_USE_KEY).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(TOOLTIP_ITEMS_LOST_KEY).withStyle(ChatFormatting.RED));
    }

    /**
     * Whether villager is an eligible conversion target for the current mode
     * <p>
     * False when the villager already matches what mode would produce.
     */
    public static boolean isEligibleForConversion(@Nonnull Villager villager, @Nonnull TotemMode mode) {
        return !mode.isAlreadyTargetType(villager instanceof BaseVillager, villager.isNoAi());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level,
                                                  @Nonnull Player player,
                                                  @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Cycling is right click only
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide()) {
            // Safe to mutate the held stack here: the use key cannot re-fire while isUsingItem() is true
            setMode(stack, getMode(stack).next());
            level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS);
        }

        player.getCooldowns().addCooldown(this, COOLDOWN.getTicksAsInt());

        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult interactLivingEntity(@Nonnull ItemStack stack,
                                                  @Nonnull Player player,
                                                  @Nonnull LivingEntity target,
                                                  @Nonnull InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !(target instanceof Villager villager) || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        // An ineligible target is refused silently. The held-item HUD names the villager's type before the
        // click, and this path is reached on every stray sneak-click across an already-converted settlement,
        // where a cue would be noise rather than news.
        if (!isEligibleForConversion(villager, getMode(stack))) {
            return InteractionResult.CONSUME;
        }

        TotemTargetAttachment.setTarget(player, villager.getId());
        player.startUsingItem(hand);

        if (!player.level().isClientSide()) {
            villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ClockTicks.seconds(3).getTicksAsInt(), 10, false, false));
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return ClockTicks.seconds(2.5).getTicksAsInt();
    }

    @Override
    public UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity playerEntity, @Nonnull ItemStack stack, int remainingUseDuration) {
        if (playerEntity instanceof Player player) {
            OptionalInt targetId = TotemTargetAttachment.getTarget(player);
            if (targetId.isEmpty() || !(level.getEntity(targetId.getAsInt()) instanceof Villager villager)) {
                player.stopUsingItem();
                return;
            }

            if (villager.distanceToSqr(player) > MAX_DISTANCE_SQUARED) {
                player.stopUsingItem();
                return;
            }

            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                TotemMode mode = getMode(stack);

                int ticksUsed = this.getUseDuration(stack, player) - remainingUseDuration;
                double height = (villager.getBbHeight() * ticksUsed) / 50.0;
                double angle = ticksUsed * 0.5;
                double radius = 0.8;

                double x = villager.getX() + radius * Math.cos(angle);
                double y = villager.getY() + height;
                double z = villager.getZ() + radius * Math.sin(angle);

                serverLevel.sendParticles(new DustParticleOptions(mode.hue(), 1.0f), x, y, z, 1, 0, 0, 0, 0);

                if (remainingUseDuration % 10 == 0) {
                    level.playSound(null, villager.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.PLAYERS, 0.5F, 0.5F + (ticksUsed / 50.0F));
                }
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) {
            return stack;
        }

        Optional<Villager> target = resolveChannelTarget(player, level);
        if (target.isEmpty()) {
            return stack;
        }

        Villager villager = target.get();
        TotemMode mode = getMode(stack);

        // interactLivingEntity already rejects an ineligible target before the channel starts; this only
        // re-fires if some other actor changed the villager's type during the 2.5s channel.
        if (!isEligibleForConversion(villager, mode)) {
            rejectConversion(level, villager);
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable(ALREADY_TARGET_TYPE_KEY).withStyle(ChatFormatting.RED), true);
            }
            return stack;
        }

        player.getCooldowns().addCooldown(this, COOLDOWN.getTicksAsInt());

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 2.0F),
                    villager.getX(), villager.getY() + 1.0, villager.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
            level.playSound(null, villager.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.0F);

            convertVillager(serverLevel, villager, mode);
        }

        return stack;
    }

    /**
     * The villager this channel is still locked onto, or empty if the lock was dropped or the target has since
     * moved out of range.
     */
    private static Optional<Villager> resolveChannelTarget(@Nonnull Player player, @Nonnull Level level) {
        OptionalInt targetId = TotemTargetAttachment.getTarget(player);
        if (targetId.isEmpty()) {
            return Optional.empty();
        }

        return level.getEntity(targetId.getAsInt()) instanceof Villager villager
                && villager.distanceToSqr(player) <= MAX_DISTANCE_SQUARED ? Optional.of(villager) : Optional.empty();
    }

    private void rejectConversion(@Nonnull Level level, @Nonnull Villager villager) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(new DustParticleOptions(new Vector3f(0.85F, 0.0F, 0.0F), 1.5F),
                villager.getX(), villager.getY() + 1.0, villager.getZ(), 20, 0.35, 0.35, 0.35, 0.02);
        serverLevel.playSound(null, villager.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.4F, 0.6F);
    }

    @Override
    public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity livingEntity, int timeCharged) {
        if (livingEntity instanceof Player player) {
            TotemTargetAttachment.clearTarget(player);
        }
    }

    private void convertVillager(ServerLevel level, Villager oldVillager, TotemMode mode) {
        if (mode.convertsToVanilla()) {
            VillagerConversionUtil.convertToVanilla(level, oldVillager, mode.keepsVanillaVillagerInStasis());
        } else {
            // Treat totem conversion as world-gen origin
            VillagerConversionUtil.convertToSettlements(level, oldVillager, OriginType.WORLDGEN);
        }
    }

}
