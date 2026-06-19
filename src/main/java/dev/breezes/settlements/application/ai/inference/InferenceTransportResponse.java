package dev.breezes.settlements.application.ai.inference;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * Non-throwing transport result. Empty body means the capability should fall back.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class InferenceTransportResponse {

    private final String body;
    private final int statusCode;

    public static InferenceTransportResponse success(String body, int statusCode) {
        return new InferenceTransportResponse(body, statusCode);
    }

    public static InferenceTransportResponse miss() {
        return new InferenceTransportResponse(null, 0);
    }

    public Optional<String> body() {
        return Optional.ofNullable(this.body).filter(value -> !value.isBlank());
    }

}
