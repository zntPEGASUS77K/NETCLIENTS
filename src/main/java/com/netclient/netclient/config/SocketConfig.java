package com.netclient.netclient.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import com.netclient.netclient.services.ClientService;

@Configuration
@EnableWebSocket
public class SocketConfig implements WebSocketConfigurer {

    private final ClientService clientService;

    public SocketConfig(ClientService clientService) {
        this.clientService = clientService;
    }

    @Bean
    public ClientWebSocketHandler clientWebSocketHandler() {
        return new ClientWebSocketHandler(clientService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(clientWebSocketHandler(), "/client").setAllowedOrigins("*");
    }
}