package org.flossware.jplatform.api;

/**
 * Base unchecked exception for all platform-java operations.
 * All platform-specific exceptions extend this class.
 * <p>
 * This is an unchecked exception (extends RuntimeException) to provide flexibility
 * in error handling without forcing applications to catch every possible exception.
 *
 * @since 1.2
 */
public class PlatformException extends RuntimeException {

    /**
     * Constructs a new platform exception with the specified detail message.
     *
     * @param message the detail message
     */
    public PlatformException(String message) {
        super(message);
    }

    /**
     * Constructs a new platform exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public PlatformException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new platform exception with the specified cause.
     *
     * @param cause the cause
     */
    public PlatformException(Throwable cause) {
        super(cause);
    }
}
