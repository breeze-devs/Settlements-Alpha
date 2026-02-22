package dev.breezes.settlements.shared.util;

public class SettlementsException extends RuntimeException {

    public SettlementsException() {
        super();
    }

    public SettlementsException(String message) {
        super(message);
    }

    public SettlementsException(String message, Throwable cause) {
        super(message, cause);
    }

}
