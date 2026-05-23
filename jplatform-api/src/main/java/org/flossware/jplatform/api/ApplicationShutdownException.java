package org.flossware.jplatform.api;

/**
 * Thrown when an application fails to stop gracefully.
 * This exception indicates that the application's stop() method failed,
 * or that the platform could not transition the application to STOPPED state.
 *
 * @since 1.2
 */
public class ApplicationShutdownException extends PlatformException {

    private final String applicationId;

    /**
     * Constructs a new application shutdown exception.
     *
     * @param applicationId the ID of the application that failed to stop
     * @param message the detail message
     */
    public ApplicationShutdownException(String applicationId, String message) {
        super(message);
        this.applicationId = applicationId;
    }

    /**
     * Constructs a new application shutdown exception with a cause.
     *
     * @param applicationId the ID of the application that failed to stop
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ApplicationShutdownException(String applicationId, String message, Throwable cause) {
        super(message, cause);
        this.applicationId = applicationId;
    }

    /**
     * Returns the ID of the application that failed to stop.
     *
     * @return the application ID
     */
    public String getApplicationId() {
        return applicationId;
    }
}
