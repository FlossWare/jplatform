package org.flossware.jplatform.rest.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;
import org.flossware.jplatform.api.PlatformApiServer;
import org.flossware.jplatform.api.ServerShutdownException;
import org.flossware.jplatform.api.ServerStartupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Netty-based REST API server implementation.
 * Provides high-performance HTTP server using Netty framework.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Uses Netty's non-blocking I/O</li>
 *   <li>Supports route registration</li>
 *   <li>JSON request/response handling</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 *
 * <p>Thread Safety: This class is thread-safe.
 *
 * @since 1.1
 */
public class NettyApiServer implements PlatformApiServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyApiServer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final NettyApiServerConfig config;
    private final Map<String, Function<String, String>> routes;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;

    /**
     * Constructs a new Netty API server.
     *
     * @param config the server configuration
     */
    public NettyApiServer(NettyApiServerConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Config must not be null");
        }
        this.config = config;
        this.routes = new ConcurrentHashMap<>();
    }

    /**
     * Registers a route handler.
     *
     * @param path the request path
     * @param handler the handler function
     */
    public void addRoute(String path, Function<String, String> handler) {
        routes.put(path, handler);
    }

    /**
     * Removes a route handler.
     *
     * @param path the request path
     */
    public void removeRoute(String path) {
        routes.remove(path);
    }

    @Override
    public void start() throws ServerStartupException {
        if (running) {
            return;
        }

        try {
            bossGroup = new NioEventLoopGroup(config.getBossThreads());
            workerGroup = config.getWorkerThreads() > 0
                ? new NioEventLoopGroup(config.getWorkerThreads())
                : new NioEventLoopGroup();

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new HttpObjectAggregator(config.getMaxContentLength()));
                        pipeline.addLast(new HttpRequestHandler(routes));
                    }
                })
                .option(ChannelOption.SO_BACKLOG, config.getBacklog())
                .childOption(ChannelOption.SO_KEEPALIVE, config.isKeepAlive());

            ChannelFuture future = bootstrap.bind(config.getHost(), config.getPort()).sync();
            serverChannel = future.channel();
            running = true;

            logger.info("Netty API server started on {}:{}", config.getHost(), config.getPort());
        } catch (Exception e) {
            logger.error("Failed to start Netty API server", e);
            throw new ServerStartupException("Failed to start server", config.getPort(), e);
        }
    }

    @Override
    public void stop() throws ServerShutdownException {
        if (!running) {
            return;
        }

        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }

            if (workerGroup != null) {
                workerGroup.shutdownGracefully().sync();
            }

            if (bossGroup != null) {
                bossGroup.shutdownGracefully().sync();
            }

            running = false;
            logger.info("Netty API server stopped");
        } catch (Exception e) {
            logger.error("Failed to stop Netty API server", e);
            throw new ServerShutdownException("Failed to stop server", e);
        }
    }

    @Override
    public int getPort() {
        return config.getPort();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    /**
     * Returns the server configuration.
     *
     * @return the configuration
     */
    public NettyApiServerConfig getConfig() {
        return config;
    }

    /**
     * Returns the registered routes.
     *
     * @return the routes map
     */
    Map<String, Function<String, String>> getRoutes() {
        return routes;
    }

    /**
     * HTTP request handler.
     */
    private static class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        private final Map<String, Function<String, String>> routes;

        HttpRequestHandler(Map<String, Function<String, String>> routes) {
            this.routes = routes;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
            String uri = request.uri();
            String content = request.content().toString(CharsetUtil.UTF_8);

            Function<String, String> handler = routes.get(uri);

            FullHttpResponse response;
            if (handler != null) {
                try {
                    String responseBody = handler.apply(content);
                    response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.OK,
                        Unpooled.copiedBuffer(responseBody, CharsetUtil.UTF_8)
                    );
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                } catch (Exception e) {
                    response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        Unpooled.copiedBuffer("{\"error\":\"" + e.getMessage() + "\"}", CharsetUtil.UTF_8)
                    );
                    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
                }
            } else {
                response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.NOT_FOUND,
                    Unpooled.copiedBuffer("{\"error\":\"Route not found\"}", CharsetUtil.UTF_8)
                );
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            }

            response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

            if (HttpUtil.isKeepAlive(request)) {
                response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ctx.writeAndFlush(response);

            if (!HttpUtil.isKeepAlive(request)) {
                ctx.close();
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Error handling request", cause);
            ctx.close();
        }
    }
}
