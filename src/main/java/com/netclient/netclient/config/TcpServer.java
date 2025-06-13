package com.netclient.netclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netclient.netclient.model.ClientDTO;
import com.netclient.netclient.services.ClientService;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class TcpServer {
    private final ClientService clientService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final int PORT = 9090;

    public TcpServer(ClientService clientService) {
        this.clientService = clientService;
        startServer();
    }

    private void startServer() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Serveur TCP démarré sur le port " + PORT);
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(new ClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (InputStream in = socket.getInputStream();
                 OutputStream out = socket.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                String json = new String(buffer, 0, bytesRead);
                Map<String, Object> data = objectMapper.readValue(json, Map.class);
                String operation = (String) data.get("operation");
                ClientDTO client = objectMapper.convertValue(data.get("client"), ClientDTO.class);

                String response;
                switch (operation) {
                    case "ADD":
                        clientService.createClient(client);
                        response = "Données reçues";
                        break;
                    case "UPDATE":
                        clientService.updateClient(client.getClientId(), client);
                        response = "Enregistrement modifié";
                        break;
                    case "DELETE":
                        clientService.deleteClient(client.getClientId());
                        response = "Enregistrement supprimé";
                        break;
                    default:
                        response = "Opération non reconnue";
                }

                out.write(response.getBytes());
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}