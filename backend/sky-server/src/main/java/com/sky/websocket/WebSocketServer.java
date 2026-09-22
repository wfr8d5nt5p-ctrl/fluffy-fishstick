package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务端：与客户端（商家浏览器）建立长连接并双向通信
 * 路径 /ws/{userId}，如 ws://localhost:8080/ws/1
 */
@Component
@ServerEndpoint("/ws/{userId}")
@Slf4j
public class WebSocketServer {

    /**
     * 存放建立连接的Session（key为userId）
     * 每个打开当前页面的浏览器对应一个Connection
     */
    private static final Map<String, Session> connectionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立时触发
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        connectionMap.put(userId, session);
        log.info("websocket 连接建立，userId={}", userId);
    }

    /**
     * 收到客户端消息时触发
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String userId) {
        log.info("收到客户端消息，userId={}，message={}", userId, message);
    }

    /**
     * 连接关闭时触发
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        connectionMap.remove(userId);
        log.info("websocket 连接关闭，userId={}", userId);
    }

    /**
     * 连接出错时触发
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("websocket 连接出错：{}", error.getMessage());
    }

    /**
     * 向所有连接的客户端推送消息（群发）
     */
    public void sendToAllClient(String message) {
        for (String userId : connectionMap.keySet()) {
            try {
                Session session = connectionMap.get(userId);
                session.getBasicRemote().sendText(message);
                log.info("已向 userId={} 推送消息：{}", userId, message);
            } catch (IOException e) {
                log.error("向 userId={} 推送消息失败：{}", userId, e.getMessage());
            }
        }
    }

    /**
     * 向指定客户端推送消息（单发）
     */
    public void sendToClient(String userId, String message) {
        Session session = connectionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("已向 userId={} 推送消息：{}", userId, message);
            } catch (IOException e) {
                log.error("向 userId={} 推送消息失败：{}", userId, e.getMessage());
            }
        }
    }
}