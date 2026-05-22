package org.flossware.jplatform.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Parser for loading ApplicationDescriptor from YAML configuration files.
 * Uses Jackson's YAMLMapper to deserialize YAML content into ApplicationDescriptor objects.
 *
 * <p>This parser supports standard YAML syntax and maps configuration elements to
 * the ApplicationDescriptor domain model. Fields are deserialized through the
 * ApplicationDescriptorDTO intermediary layer.</p>
 *
 * <p>Example YAML configuration:</p>
 * <pre>{@code
 * applicationId: my-app
 * name: My Application
 * version: 1.0.0
 * mainClass: com.example.MyApp
 * classpathEntries:
 *   - file:///app/lib/app.jar
 *   - file:///app/lib/deps.jar
 * threadPoolConfig:
 *   corePoolSize: 5
 *   maxPoolSize: 20
 *   keepAliveTimeSeconds: 120
 *   queueCapacity: 200
 * securityConfig:
 *   allowReflection: true
 *   allowNativeCode: false
 *   filePermissions:
 *     - path: /tmp/-
 *       actions: read,write
 *   socketPermissions:
 *     - host: localhost:8080
 *       actions: connect
 * resourceConfig:
 *   maxHeapMB: 512
 *   maxThreads: 50
 *   maxCpuTimeSeconds: 60
 * properties:
 *   env: production
 *   debug: false
 * enableMessaging: true
 * }</pre>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * ApplicationDescriptorParser parser = new YamlDescriptorParser();
 * ApplicationDescriptor descriptor = parser.parseFile(Paths.get("app.yaml"));
 * applicationManager.deploy(descriptor);
 * }</pre>
 *
 * @see AbstractDescriptorParser
 * @see ApplicationDescriptorDTO
 * @see org.flossware.jplatform.api.ApplicationDescriptor
 */
public class YamlDescriptorParser extends AbstractDescriptorParser {

    /**
     * Constructs a new YAML descriptor parser.
     * Initializes the parser with a YAMLMapper for processing YAML content.
     */
    public YamlDescriptorParser() {
        super();
    }

    /**
     * Creates a YAMLMapper for parsing YAML content.
     * Configured to use private fields via @JsonProperty annotations.
     *
     * @return a new YAMLMapper instance
     */
    @Override
    protected ObjectMapper createObjectMapper() {
        YAMLMapper mapper = new YAMLMapper();
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }

    /**
     * Returns the format supported by this parser.
     *
     * @return Format.YAML
     */
    @Override
    public Format getSupportedFormat() {
        return Format.YAML;
    }
}
