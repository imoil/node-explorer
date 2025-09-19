package com.example.treeapi.config;

import com.example.treeapi.handler.WebSocketUpdateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSocketUpdateHandler webSocketUpdateHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Map the handler to the "/ws" endpoint and allow all origins for development
        registry.addHandler(webSocketUpdateHandler, "/ws").setAllowedOrigins("*");
    }
}
