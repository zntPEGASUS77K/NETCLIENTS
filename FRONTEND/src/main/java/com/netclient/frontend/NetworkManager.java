package com.netclient.frontend;

import javafx.application.Platform;

import java.net.InetAddress;

public class NetworkManager {
    private final ClientController controller;
    private volatile boolean networkAvailable;

    public NetworkManager(ClientController controller) {
        this.controller = controller;
        networkAvailable = false;
        checkNetworkStatus();
    }

    public void checkNetworkStatus() {
        new Thread(() -> {
            while (true) {
                boolean isAvailable;
                try {
                    isAvailable = InetAddress.getByName("google.com").isReachable(1000);
                    updateNetworkStatus(isAvailable);
                } catch (Exception e) {
                    isAvailable = false;
                    updateNetworkStatus(false);
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    public void forceOffline() {
        updateNetworkStatus(false);
    }

    public void checkReconnection() {
        try {
            boolean isAvailable = InetAddress.getByName("google.com").isReachable(1000);
            updateNetworkStatus(isAvailable);
        } catch (Exception e) {
            updateNetworkStatus(false);
        }
    }

    private void updateNetworkStatus(boolean isAvailable) {
        if (networkAvailable != isAvailable) {
            networkAvailable = isAvailable;
            Platform.runLater(() -> {
                controller.updateStatus(isAvailable ? "Réseau disponible" : "Réseau indisponible");
                controller.updateNetworkIndicator(isAvailable);
                if (isAvailable) {
                    controller.syncPendingClients();
                }
            });
        }
    }

    public boolean isNetworkAvailable() {
        return networkAvailable;
    }
}