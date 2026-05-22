package org.flossware.jplatform.rest;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.flossware.jplatform.api.ApiServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

/**
 * HTTP filter for API key authentication.
 * Validates the API key header if authentication is enabled in the configuration.
 * Returns 401 Unauthorized if authentication fails, otherwise passes the request through.
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * ApiServerConfig config = ApiServerConfig.builder()
 *     .enableAuth(true)
 *     .apiKey("secret-key-123")
 *     .apiKeyHeader("X-API-Key")
 *     .build();
 *
 * // Filter will check for "X-API-Key: secret-key-123" header
 * }</pre>
 *
 * @see ApiServerConfig
 * @see JdkHttpApiServer
 */
public class ApiAuthFilter extends Filter {

    private static final Logger logger = LoggerFactory.getLogger(ApiAuthFilter.class);

    private final ApiServerConfig config;

    /**
     * Constructs a new API authentication filter.
     *
     * @param config the API server configuration containing auth settings
     */
    public ApiAuthFilter(ApiServerConfig config) {
        this.config = config;
    }

    /**
     * Filters the HTTP request and checks authentication if enabled.
     * Allows OPTIONS requests to pass through for CORS preflight.
     *
     * @param exchange the HTTP exchange
     * @param chain the filter chain to continue processing
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
        // Allow OPTIONS requests for CORS preflight
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            chain.doFilter(exchange);
            return;
        }

        // If auth is disabled, pass through
        if (!config.isEnableAuth()) {
            chain.doFilter(exchange);
            return;
        }

        // Check API key
        String apiKeyHeader = config.getApiKeyHeader();
        String providedKey = exchange.getRequestHeaders().getFirst(apiKeyHeader);

        if (providedKey == null || !providedKey.equals(config.getApiKey())) {
            logger.warn("Unauthorized request from {}: missing or invalid API key",
                    exchange.getRemoteAddress());
            sendUnauthorized(exchange);
            return;
        }

        // Auth successful, continue
        chain.doFilter(exchange);
    }

    /**
     * Sends a 401 Unauthorized response.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    private void sendUnauthorized(HttpExchange exchange) throws IOException {
        String response = "{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing API key\",\"status\":401}";
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(401, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    /**
     * Returns a description of this filter.
     *
     * @return the filter description
     */
    @Override
    public String description() {
        return "API Key Authentication Filter";
    }
}
