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

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class TcpClient {
    private final ClientController controller;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9090;
    private static final String API_BASE_URL = "http://localhost:8078/api/v1/clients";

    public TcpClient(ClientController controller) {
        this.controller = controller;
        fetchAllClients();
    }

    public boolean sendOperation(String operation, ClientDTO client) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            Map<String, Object> data = Map.of("operation", operation, "client", client);
            String json = objectMapper.writeValueAsString(data);
            out.write(json.getBytes());
            out.flush();

            byte[] buffer = new byte[1024];
            int bytesRead = in.read(buffer);
            String response = new String(buffer, 0, bytesRead);

            controller.updateStatus(response);

            // Correction ici : prendre en compte "supprimé" comme réponse valide
            String lowerResponse = response.toLowerCase();
            return lowerResponse.contains("modifié") ||
                    lowerResponse.contains("reçues") ||
                    lowerResponse.contains("supprimé") ||
                    lowerResponse.contains("supprimée");

        } catch (IOException e) {
            controller.updateStatus("Erreur lors de l'envoi: " + e.getMessage());
            return false;
        }
    }

    public void deleteClient(Long id) {
        try {
            HttpDelete request = new HttpDelete(API_BASE_URL + "/" + id);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    controller.updateStatus("Enregistrement supprimé");
                    fetchAllClients();
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
                    fetchAllClients();
                } else {
                    controller.updateStatus("Erreur lors de la mise à jour: " + statusCode);
                }
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    public void fetchAllClients() {
        try {
            HttpGet request = new HttpGet(API_BASE_URL);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    String json = EntityUtils.toString(response.getEntity());
                    List<ClientDTO> clients = objectMapper.readValue(json, new TypeReference<List<ClientDTO>>() {});
                    controller.updateClientTable(clients);
                } else {
                    controller.updateStatus("Erreur lors de la récupération: " + statusCode);
                }
            }
        } catch (Exception e) {
            controller.updateStatus("Erreur lors de la récupération: " + e.getMessage());
        }
    }

    public List<ClientDTO> fetchAllClientsFromServer() {
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
}
