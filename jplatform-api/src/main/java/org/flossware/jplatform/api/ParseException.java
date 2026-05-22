package org.flossware.jplatform.api;

/**
 * Exception thrown when parsing an ApplicationDescriptor fails.
 * This can occur due to invalid syntax, missing required fields,
 * or incompatible configuration values.
 *
 * @see ApplicationDescriptorParser
 */
public class ParseException extends Exception {

    /**
     * Constructs a new parse exception with the specified message.
     *
     * @param message the detail message
     */
    public ParseException(String message) {
        super(message);
    }

    /**
     * Constructs a new parse exception with the specified message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
