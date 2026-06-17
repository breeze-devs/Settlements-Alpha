package dev.breezes.settlements.application.ai.gossip;

/**
 * Lifecycle phases for a {@link GossipSession}
 * <p>
 * The sequence is: INVITE_SENT → ACCEPTED → COMPLETED (or ABORTED at any point)
 */
public enum GossipPhase {

    /**
     * The initiator has registered the session; the receiver has not yet admitted the cue.
     */
    INVITE_SENT,

    /**
     * The receiver's arbiter admitted the matching gossip cue; the exchange is in progress.
     */
    ACCEPTED,

    /**
     * Both villagers completed their cue and the knowledge copy landed. Terminal.
     */
    COMPLETED,

    /**
     * The session was abandoned (timeout or one party left range). Terminal.
     */
    ABORTED,
    ;

}
