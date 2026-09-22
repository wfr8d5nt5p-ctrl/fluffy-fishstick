package com.sky.task;

import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WebSocket定时推送任务：每隔一段时间向客户端推送消息
 */
@Component
@Slf4j
public class WebSocketTask {

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 每隔5秒向所有客户端推送当前时间（演示用，正式业务可替换为推送订单状态等）
     */
    @Scheduled(cron = "0/5 * * * * ?")
    public void sendMessageToAllClient() {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        webSocketServer.sendToAllClient("来自服务端的消息：" + now);
        log.info("定时任务：向所有客户端推送当前时间 {}", now);
    }
}