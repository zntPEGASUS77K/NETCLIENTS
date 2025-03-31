package com.netclient.frontend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.List;

public class WebSocketClient {
    private final ClientController controller;
    private WebSocketSession session;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final String API_BASE_URL = "http://localhost:8078/api/v1/clients";

    public WebSocketClient(ClientController controller) {
        this.controller = controller;
        connect();
    }

    private void connect() {
        StandardWebSocketClient client = new StandardWebSocketClient();
        client.doHandshake(new CustomWebSocketHandler(), "ws://localhost:8078/client");
    }

    public void sendClientData(ClientDTO client) {
        try {
            if (session != null && session.isOpen()) {
                String json = objectMapper.writeValueAsString(client);
                session.sendMessage(new TextMessage(json));
            } else {
                controller.updateStatus("Erreur: Session WebSocket non connectée");
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de l'envoi: " + e.getMessage());
        }
    }

    public void deleteClient(Long id) {
        try {
            HttpDelete request = new HttpDelete(API_BASE_URL + "/" + id);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    controller.updateStatus("Enregistrement supprimé");
                } else {
                    controller.updateStatus("Erreur lors de la suppression: " + statusCode);
                }
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de la suppression: " + e.getMessage());
        }
    }

    public void updateClient(ClientDTO client) {
        try {
            HttpPut request = new HttpPut(API_BASE_URL + "/" + client.getClientId());
            request.setHeader("Content-Type", "application/json");
            String json = objectMapper.writeValueAsString(client);
            request.setEntity(new StringEntity(json));
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    controller.updateStatus("Enregistrement modifié");
                } else {
                    controller.updateStatus("Erreur lors de la mise à jour: " + statusCode);
                }
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    public List<ClientDTO> getAllClients() {
        try {
            HttpGet request = new HttpGet(API_BASE_URL);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    return objectMapper.readValue(json, new TypeReference<List<ClientDTO>>() {});
                } else {
                    controller.updateStatus("Erreur lors de la récupération: " + statusCode);
                    return List.of();
                }
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de la récupération: " + e.getMessage());
            return List.of();
        }
    }

    private class CustomWebSocketHandler extends TextWebSocketHandler {
        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            WebSocketClient.this.session = session;
            controller.updateStatus("Connecté au serveur");
            List<ClientDTO> clients = getAllClients();
            controller.updateClientTable(clients);
        }

        @Override
        public void handleTextMessage(WebSocketSession session, TextMessage message) {
            controller.updateStatus(message.getPayload());
            List<ClientDTO> clients = getAllClients();
            controller.updateClientTable(clients);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            controller.updateStatus("Erreur WebSocket: " + exception.getMessage());
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
            controller.updateStatus("Déconnecté du serveur");
            WebSocketClient.this.session = null;
        }
    }
}