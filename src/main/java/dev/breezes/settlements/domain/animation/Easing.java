package dev.breezes.settlements.domain.animation;

/**
 * Curves normalized keyframe progress before type-specific interpolation.
 */
public enum Easing {

    LINEAR {
        @Override
        public float apply(float t) {
            return Math.clamp(t, 0.0F, 1.0F);
        }
    },
    EASE_IN {
        @Override
        public float apply(float t) {
            float clamped = Math.clamp(t, 0.0F, 1.0F);
            return clamped * clamped;
        }
    },
    EASE_OUT {
        @Override
        public float apply(float t) {
            float clamped = Math.clamp(t, 0.0F, 1.0F);
            return 1.0F - ((1.0F - clamped) * (1.0F - clamped));
        }
    },
    EASE_IN_OUT {
        @Override
        public float apply(float t) {
            float clamped = Math.clamp(t, 0.0F, 1.0F);
            if (clamped < 0.5F) {
                return 2.0F * clamped * clamped;
            }

            float inverse = -2.0F * clamped + 2.0F;
            return 1.0F - ((inverse * inverse) / 2.0F);
        }
    },
    STEP {
        @Override
        public float apply(float t) {
            return Math.clamp(t, 0.0F, 1.0F) >= 1.0F ? 1.0F : 0.0F;
        }
    },
    CUBIC {
        @Override
        public float apply(float t) {
            float clamped = Math.clamp(t, 0.0F, 1.0F);
            return clamped * clamped * (3.0F - (2.0F * clamped));
        }
    };

    public abstract float apply(float t);

}
