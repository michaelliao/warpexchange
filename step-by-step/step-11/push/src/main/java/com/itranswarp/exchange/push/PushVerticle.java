package com.itranswarp.exchange.push;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itranswarp.exchange.bean.AuthToken;
import com.itranswarp.exchange.message.NotificationMessage;
import com.itranswarp.exchange.util.JsonUtil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;

public class PushVerticle extends AbstractVerticle {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String hmacKey;

    private final int serverPort;

    PushVerticle(String hmacKey, int serverPort) {
        this.hmacKey = hmacKey;
        this.serverPort = serverPort;
    }

    /**
     * All handlers.
     */
    private final Map<String, Boolean> handlersSet = new ConcurrentHashMap<>(1000);

    /**
     * userId -> set of handlers.
     */
    private final Map<Long, Set<String>> userToHandlersMap = new ConcurrentHashMap<>(1000);

    /**
     * handler -> userId.
     */
    private final Map<String, Long> handlerToUserMap = new ConcurrentHashMap<>(1000);

    @Override
    public void start() {
        // 创建VertX HttpServer:
        HttpServer server = vertx.createHttpServer();

        // 创建路由:
        Router router = Router.router(vertx);

        // 处理请求 GET /notification:
        router.get("/notification").handler(requestHandler -> {
            HttpServerRequest request = requestHandler.request();
            // 从token参数解析userId:
            Supplier<Long> supplier = () -> {
                String tokenStr = request.getParam("token");
                if (tokenStr != null && !tokenStr.isEmpty()) {
                    AuthToken token = AuthToken.fromSecureString(tokenStr, this.hmacKey);
                    if (!token.isExpired()) {
                        return token.userId();
                    }
                }
                return null;
            };
            final Long userId = supplier.get();
            logger.info("parse user id from token: {}", userId);
            // 将连接升级到WebSocket:
            request.toWebSocket(ar -> {
                if (ar.succeeded()) {
                    initWebSocket(ar.result(), userId);
                }
            });
        });

        // 处理请求 GET /actuator/health:
        router.get("/actuator/health").respond(
                ctx -> ctx.response().putHeader("Content-Type", "application/json").end("{\"status\":\"UP\"}"));

        // 处理其他请求:
        router.get().respond(ctx -> ctx.response().setStatusCode(404).setStatusMessage("No Route Found").end());

        server.requestHandler(router).listen(this.serverPort, result -> {
            if (result.succeeded()) {
                logger.info("Vertx started on port(s): {} (http) with context path ''", this.serverPort);
            } else {
                logger.error("Start http server failed on port " + this.serverPort, result.cause());
                vertx.close();
                System.exit(1);
            }
        });
    }

    void initWebSocket(ServerWebSocket websocket, Long userId) {
        String handlerId = websocket.textHandlerID();
        logger.info("websocket accept userId: " + userId + ", handlerId: " + handlerId);
        // handle text message:
        websocket.textMessageHandler(str -> {
            logger.info("text message: " + str);
        });
        websocket.exceptionHandler(t -> {
            logger.error("websocket error: " + t.getMessage(), t);
        });
        // on close:
        websocket.closeHandler(e -> {
            unsubscribeClient(handlerId);
            unsubscribeUser(handlerId, userId);
            logger.info("websocket closed: " + handlerId);
        });
        subscribeClient(handlerId);
        subscribeUser(handlerId, userId);
        // send welcome message:
        if (userId == null) {
            websocket.writeTextMessage(
                    "{\"type\":\"status\",\"status\":\"connected\",\"message\":\"connected as anonymous user\"}");
        } else {
            websocket.writeTextMessage(
                    "{\"type\":\"status\",\"status\":\"connected\",\"message\":\"connected as user\",\"userId\":"
                            + userId + "}");
        }
    }

    public void broadcast(String text) {
        NotificationMessage message = null;
        try {
            message = JsonUtil.readJson(text, NotificationMessage.class);
        } catch (Exception e) {
            logger.error("invalid message format: {}", text);
            return;
        }
        if (message.userId == null) {
            if (logger.isInfoEnabled()) {
                logger.info("try broadcast message to all: {}", text);
            }
            // broadcast to all:
            EventBus eb = vertx.eventBus();
            for (String handler : this.handlersSet.keySet()) {
                eb.send(handler, text);
            }
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("try broadcast message to user {}: {}", message.userId, text);
            }
            // broadcast to single user:
            Set<String> handlers = this.userToHandlersMap.get(message.userId);
            if (handlers != null) {
                EventBus eb = vertx.eventBus();
                for (String handler : handlers) {
                    eb.send(handler, text);
                }
            }
        }
    }

    void subscribeClient(String handlerId) {
        this.handlersSet.put(handlerId, Boolean.TRUE);
    }

    void unsubscribeClient(String handlerId) {
        this.handlersSet.remove(handlerId);
    }

    void subscribeUser(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.put(handlerId, userId);
        Set<String> set = userToHandlersMap.get(userId);
        if (set == null) {
            set = new HashSet<>();
            userToHandlersMap.put(userId, set);
        }
        set.add(handlerId);
        logger.info("subscribe user {} {} ok.", userId, handlerId);
    }

    void unsubscribeUser(String handlerId, Long userId) {
        if (userId == null) {
            return;
        }
        handlerToUserMap.remove(handlerId);
        Set<String> set = userToHandlersMap.get(userId);
        if (set != null) {
            set.remove(handlerId);
            if (set.isEmpty()) {
                userToHandlersMap.remove(userId);
                logger.info("unsubscribe user {} {} ok: cleared.", userId, handlerId);
            } else {
                logger.info("unsubscribe user {} {} ok: but still others online.", userId, handlerId);
            }
        }
    }
}
