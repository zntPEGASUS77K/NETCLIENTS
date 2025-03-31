package com.netclient.netclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netclient.netclient.model.ClientDTO;
import com.netclient.netclient.services.ClientService;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ClientWebSocketHandler extends TextWebSocketHandler {

    private final ClientService clientService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientWebSocketHandler(ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ClientDTO clientDTO = objectMapper.readValue(message.getPayload(), ClientDTO.class);
        clientService.createClient(clientDTO);
        session.sendMessage(new TextMessage("Données reçues"));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.sendMessage(new TextMessage("Connexion établie"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String errorMessage = "Erreur: " + exception.getMessage();
        session.sendMessage(new TextMessage(errorMessage));
    }
}