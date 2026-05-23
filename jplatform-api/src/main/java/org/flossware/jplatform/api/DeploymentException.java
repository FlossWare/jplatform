package org.flossware.jplatform.api;

/**
 * Thrown when application deployment or undeployment fails.
 * This exception indicates failures during:
 * <ul>
 *   <li>Application deployment (classloader creation, resource allocation)</li>
 *   <li>Application undeployment (resource cleanup, classloader disposal)</li>
 *   <li>Hot reload operations</li>
 * </ul>
 *
 * @since 1.2
 */
public class DeploymentException extends PlatformException {

    private final String applicationId;

    /**
     * Constructs a new deployment exception.
     *
     * @param applicationId the ID of the application
     * @param message the detail message
     */
    public DeploymentException(String applicationId, String message) {
        super(message);
        this.applicationId = applicationId;
    }

    /**
     * Constructs a new deployment exception with a cause.
     *
     * @param applicationId the ID of the application
     * @param message the detail message
     * @param cause the underlying cause
     */
    public DeploymentException(String applicationId, String message, Throwable cause) {
        super(message, cause);
        this.applicationId = applicationId;
    }

    /**
     * Returns the ID of the application.
     *
     * @return the application ID
     */
    public String getApplicationId() {
        return applicationId;
    }
}
