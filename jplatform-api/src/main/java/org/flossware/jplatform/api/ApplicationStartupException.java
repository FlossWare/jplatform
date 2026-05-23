package org.flossware.jplatform.api;

/**
 * Thrown when an application fails to start.
 * This exception indicates that the application's start() method failed,
 * or that the platform could not transition the application to RUNNING state.
 *
 * @since 1.2
 */
public class ApplicationStartupException extends PlatformException {

    private final String applicationId;

    /**
     * Constructs a new application startup exception.
     *
     * @param applicationId the ID of the application that failed to start
     * @param message the detail message
     */
    public ApplicationStartupException(String applicationId, String message) {
        super(message);
        this.applicationId = applicationId;
    }

    /**
     * Constructs a new application startup exception with a cause.
     *
     * @param applicationId the ID of the application that failed to start
     * @param message the detail message
     * @param cause the underlying cause
     */
    public ApplicationStartupException(String applicationId, String message, Throwable cause) {
        super(message, cause);
        this.applicationId = applicationId;
    }

    /**
     * Returns the ID of the application that failed to start.
     *
     * @return the application ID
     */
    public String getApplicationId() {
        return applicationId;
    }
}
