package dev.breezes.settlements.domain.animation;

import dev.breezes.settlements.domain.presentation.ArmConfiguration;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;

public final class LayerStack {

    private static final List<AnimationLayerRole> SAMPLE_ORDER = List.of(
            AnimationLayerRole.BASE,
            AnimationLayerRole.ACTION);
    private static final List<AnimationLayerRole> ARM_CONFIGURATION_ORDER = List.of(
            AnimationLayerRole.ACTION,
            AnimationLayerRole.BASE);

    private final EnumMap<AnimationLayerRole, AnimationLayer> layersByRole = new EnumMap<>(AnimationLayerRole.class);
    private final IdleLifeAnimator idleLifeAnimator;
    private final LocomotionAnimator locomotionAnimator;
    private final UmbrellaAnimator umbrellaAnimator;
    private final int entityId;

    public LayerStack(@Nonnull KeyframeAnimation baseAnimation,
                      @Nonnull LayerStackAnimators animators,
                      int entityId) {
        this.layersByRole.put(AnimationLayerRole.BASE, AnimationLayer.persistent(baseAnimation, 0L));
        this.idleLifeAnimator = animators.getIdleLife();
        this.locomotionAnimator = animators.getLocomotion();
        this.umbrellaAnimator = animators.getUmbrella();
        this.entityId = entityId;
    }

    public void triggerAction(@Nonnull KeyframeAnimation animation, long gameTime) {
        AnimationLayer actionLayer = this.layersByRole.get(AnimationLayerRole.ACTION);
        if (actionLayer == null) {
            this.layersByRole.put(AnimationLayerRole.ACTION, AnimationLayer.transientAction(animation, gameTime));
            return;
        }
        actionLayer.replace(animation, gameTime, true);
    }

    public void setSustainedAction(@Nonnull KeyframeAnimation animation, long gameTime) {
        AnimationLayer actionLayer = this.layersByRole.get(AnimationLayerRole.ACTION);
        if (actionLayer == null) {
            this.layersByRole.put(AnimationLayerRole.ACTION, AnimationLayer.persistent(animation, gameTime));
            return;
        }
        actionLayer.replace(animation, gameTime, false);
    }

    public void clearAction(long gameTime) {
        AnimationLayer actionLayer = this.layersByRole.get(AnimationLayerRole.ACTION);
        if (actionLayer != null) {
            actionLayer.beginClear(gameTime);
        }
    }

    public boolean hasAction() {
        return this.layersByRole.containsKey(AnimationLayerRole.ACTION);
    }

    public AnimationFrame sample(long gameTime,
                                 float partialTicks,
                                 @Nonnull LocomotionAnimationContext locomotionContext,
                                 boolean umbrellaShouldDeploy) {
        this.expireFinishedActions(gameTime, partialTicks);

        float locomotionWeight = this.locomotionAnimator.weight(locomotionContext);
        float idleLifeWeight = 1.0F - locomotionWeight;
        IdleLifeAnimationContext idleLifeContext = this.idleLifeContext(gameTime, partialTicks);
        this.idleLifeAnimator.advance(idleLifeContext);

        AnimationFrame frame = AnimationFrame.EMPTY;
        for (AnimationLayerRole role : SAMPLE_ORDER) {
            if (role == AnimationLayerRole.ACTION) {
                // Idle-life ambience recedes as locomotion ramps in so the breather's body bob does not
                // stack onto the gait's; at a standstill locomotion weight is 0 and idle-life shows fully.
                if (idleLifeWeight > 0.0F) {
                    frame = frame.composeOver(this.idleLifeAnimator.sample(idleLifeContext), idleLifeWeight);
                }
            }
            AnimationLayer layer = this.layersByRole.get(role);
            if (layer != null) {
                frame = frame.composeOver(layer.sample(gameTime, partialTicks), layer.weight(gameTime, partialTicks));
            }
            if (role == AnimationLayerRole.BASE) {
                frame = frame.composeOver(this.locomotionAnimator.sample(locomotionContext), 1.0F);
            }
        }

        return this.composeUmbrellaOver(frame, gameTime, partialTicks, umbrellaShouldDeploy);
    }

    /**
     * Advances the umbrella state machine one observation and composes its frame over the given one.
     * Unchanged while the umbrella is fully stowed.
     */
    public AnimationFrame composeUmbrellaOver(@Nonnull AnimationFrame frame,
                                              long gameTime,
                                              float partialTicks,
                                              boolean shouldDeploy) {
        UmbrellaAnimationContext context = this.umbrellaContext(gameTime, partialTicks, shouldDeploy);
        this.umbrellaAnimator.advance(context);
        if (!this.umbrellaAnimator.isVisible(context)) {
            return frame;
        }
        return frame.composeOver(this.umbrellaAnimator.sample(context), 1.0F);
    }

    /**
     * Whether the umbrella attachment should still be rendered, reading the state this frame's
     * {@link #sample} call already advanced rather than advancing it again.
     */
    public boolean isUmbrellaVisible(long gameTime, float partialTicks) {
        return this.umbrellaAnimator.isVisible(this.umbrellaContext(gameTime, partialTicks, false));
    }

    /**
     * Resolves the discrete arm configuration top-down: the active action layer wins, then any idle-life
     * fidget, then the base layer, falling back to the locomotion gait and finally {@link ArmConfiguration#BOTH_CROSSED}.
     * Arm config is a non-blendable snap, so the first owner found is taken outright rather than mixed.
     */
    public ArmConfiguration armConfiguration(long gameTime,
                                             float partialTicks,
                                             @Nonnull LocomotionAnimationContext locomotionContext) {
        this.expireFinishedActions(gameTime, partialTicks);
        float idleLifeWeight = 1.0F - this.locomotionAnimator.weight(locomotionContext);
        IdleLifeAnimationContext idleLifeContext = this.idleLifeContext(gameTime, partialTicks);
        this.idleLifeAnimator.advance(idleLifeContext);

        AnimationLayer actionLayer = this.layersByRole.get(AnimationLayerRole.ACTION);
        if (actionLayer != null) {
            Optional<ArmConfiguration> actionConfiguration = actionLayer.activeArmConfiguration(gameTime, partialTicks);
            if (actionConfiguration.isPresent()) {
                return actionConfiguration.get();
            }
        }

        if (idleLifeWeight > 0.0F) {
            Optional<ArmConfiguration> idleLifeConfiguration = this.idleLifeAnimator.activeArmConfiguration(
                    idleLifeContext);
            if (idleLifeConfiguration.isPresent()) {
                return idleLifeConfiguration.get();
            }
        }

        // ACTION is already resolved above; this top-down pass is effectively the base-layer fallback,
        // kept as an ordered list so future persistent layers slot in by precedence.
        for (AnimationLayerRole role : ARM_CONFIGURATION_ORDER) {
            AnimationLayer layer = this.layersByRole.get(role);
            if (layer == null) {
                continue;
            }
            Optional<ArmConfiguration> armConfiguration = layer.activeArmConfiguration(gameTime, partialTicks);
            if (armConfiguration.isPresent()) {
                return armConfiguration.get();
            }
        }

        return this.locomotionAnimator.activeArmConfiguration(locomotionContext)
                .orElse(ArmConfiguration.BOTH_CROSSED);
    }

    private IdleLifeAnimationContext idleLifeContext(long gameTime, float partialTicks) {
        return new IdleLifeAnimationContext(this.entityId, gameTime, partialTicks, this.hasAction());
    }

    private UmbrellaAnimationContext umbrellaContext(long gameTime, float partialTicks, boolean shouldDeploy) {
        return new UmbrellaAnimationContext(this.entityId, gameTime, partialTicks, shouldDeploy);
    }

    private void expireFinishedActions(long gameTime, float partialTicks) {
        AnimationLayer actionLayer = this.layersByRole.get(AnimationLayerRole.ACTION);
        if (actionLayer != null && actionLayer.isExpired(gameTime, partialTicks)) {
            this.layersByRole.remove(AnimationLayerRole.ACTION);
        }
    }

}
