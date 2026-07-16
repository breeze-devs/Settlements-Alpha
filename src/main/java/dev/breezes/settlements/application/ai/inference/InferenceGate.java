package dev.breezes.settlements.application.ai.inference;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Single source of truth for whether the Settlements Inference Service (SIS) cognition lane is
 * active this session.
 * <p>
 * {@link InferenceConfig} is an immutable snapshot materialized once at server start, so this
 * predicate never changes mid-session — every registration seam is meant to consult this class
 * exactly once, at wiring time, rather than re-deriving its own endpoint/enabled check.
 */
@Singleton
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @Inject)
public final class InferenceGate {

    private final InferenceConfig config;

    /**
     * The explicit switch wins in both directions: an endpoint configured with {@code enabled=false}
     * stays off, and {@code enabled=true} with a blank endpoint also stays off (see
     * {@link #isMisconfigured()} for surfacing that second case as a startup warning).
     */
    public boolean isEnabled() {
        return this.config.enabled() && this.config.hasEndpoint();
    }

    /**
     * True when the operator opted in but left the endpoint blank — a likely config mistake worth a
     * startup warning, distinct from the ordinary "SIS turned off" case.
     */
    public boolean isMisconfigured() {
        return this.config.enabled() && !this.config.hasEndpoint();
    }

}
