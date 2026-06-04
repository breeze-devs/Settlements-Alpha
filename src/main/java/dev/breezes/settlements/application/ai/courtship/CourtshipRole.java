package dev.breezes.settlements.application.ai.courtship;

import javax.annotation.Nonnull;
import java.util.UUID;

public enum CourtshipRole {

    PRESENTER,
    RECEIVER;

    /**
     * Role is a pure function of UUIDs so both sides derive it independently without any shared flag.
     * Lower UUID is always the Presenter; this prevents mutual-invite races since only one side qualifies.
     */
    public static CourtshipRole of(@Nonnull UUID self, @Nonnull UUID partner) {
        return self.compareTo(partner) < 0 ? PRESENTER : RECEIVER;
    }

}
