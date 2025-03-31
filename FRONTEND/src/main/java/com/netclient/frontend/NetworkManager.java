package com.netclient.frontend;

import javafx.application.Platform;

import java.net.InetAddress;

public class NetworkManager {
    private final ClientController controller;
    private boolean networkAvailable;

    public NetworkManager(ClientController controller) {
        this.controller = controller;
    }

    public void checkNetworkStatus() {
        new Thread(() -> {
            while (true) {
                try {
                    InetAddress.getByName("google.com").isReachable(2000);
                    networkAvailable = true;
                    Platform.runLater(() -> controller.updateStatus("Réseau disponible"));
                } catch (Exception e) {
                    networkAvailable = false;
                    Platform.runLater(() -> controller.updateStatus("Réseau indisponible"));
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }

    public boolean isNetworkAvailable() {
        return networkAvailable;
    }
}