package com.sky.websocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类：注册 WebSocket 服务端组件
 * ServerEndpointExporter 会自动扫描 @ServerEndpoint 注解的类并注册到容器，
 * 使 WebSocketServer 能够被客户端连接
 */
@Configuration
public class WebSocketConfiguration {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}