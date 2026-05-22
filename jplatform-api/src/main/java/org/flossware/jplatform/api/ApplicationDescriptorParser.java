package org.flossware.jplatform.api;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Parser for loading ApplicationDescriptor from external configuration formats.
 * Implementations provide support for different formats (YAML, JSON, XML).
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ApplicationDescriptorParser parser = new YamlDescriptorParser();
 * ApplicationDescriptor descriptor = parser.parseFile(Paths.get("app.yaml"));
 * applicationManager.deploy(descriptor);
 * }</pre>
 *
 * @see ApplicationDescriptor
 */
public interface ApplicationDescriptorParser {

    /**
     * Parses an ApplicationDescriptor from an input stream.
     *
     * @param input the input stream containing the descriptor content
     * @return the parsed ApplicationDescriptor
     * @throws ParseException if parsing fails
     */
    ApplicationDescriptor parse(InputStream input) throws ParseException;

    /**
     * Parses an ApplicationDescriptor from a file.
     *
     * @param file the file containing the descriptor
     * @return the parsed ApplicationDescriptor
     * @throws ParseException if parsing fails or file cannot be read
     */
    ApplicationDescriptor parseFile(Path file) throws ParseException;

    /**
     * Returns the format supported by this parser.
     *
     * @return the supported format
     */
    Format getSupportedFormat();

    /**
     * Supported configuration formats.
     */
    enum Format {
        YAML, JSON, XML
    }
}
