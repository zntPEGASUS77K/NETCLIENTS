package com.netclient.frontend;

import com.netclient.frontend.utils.JsonFileManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ClientController {

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField balanceField;
    @FXML private TableView<ClientDTO> clientTable;
    @FXML private TableColumn<ClientDTO, Long> idColumn;
    @FXML private TableColumn<ClientDTO, String> nameColumn;
    @FXML private TableColumn<ClientDTO, String> addressColumn;
    @FXML private TableColumn<ClientDTO, Double> balanceColumn;
    @FXML private Label statusLabel;

    private WebSocketClient webSocketClient;
    private NetworkManager networkManager;
    private JsonFileManager jsonFileManager;
    private Stage modalStage;
    private boolean wasNetworkAvailable = false; // Track previous network state

    @FXML
    public void initialize() {
        // Check if the FXML elements are properly injected
        if (clientTable == null || idColumn == null || nameColumn == null || addressColumn == null || balanceColumn == null || statusLabel == null) {
            System.err.println("One or more FXML elements are not properly injected!");
            return;
        }

        webSocketClient = new WebSocketClient(this);
        networkManager = new NetworkManager(this);
        jsonFileManager = new JsonFileManager();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        networkManager.checkNetworkStatus();
    }

    @FXML
    private void openAddClientModal() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/netclient/frontend/add-client-modal.fxml"));
            loader.setController(this);
            Parent root = loader.load();

            modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Ajouter un Client");
            modalStage.setScene(new Scene(root));
            modalStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Erreur lors de l'ouverture du modal: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddClient() {
        if (nameField == null || addressField == null || balanceField == null) {
            updateStatus("Erreur: Les champs du formulaire ne sont pas initialisés.");
            return;
        }

        ClientDTO client = new ClientDTO();
        client.setName(nameField.getText());
        client.setAddress(addressField.getText());
        try {
            client.setBalance(Double.parseDouble(balanceField.getText()));
        } catch (NumberFormatException e) {
            updateStatus("Erreur: Le solde doit être un nombre valide.");
            return;
        }

        if (networkManager == null) {
            updateStatus("Erreur: NetworkManager non initialisé.");
            return;
        }

        if (networkManager.isNetworkAvailable()) {
            webSocketClient.sendClientData(client);
        } else {
            jsonFileManager.saveToJson(client);
            updateStatus("Réseau indisponible. Données sauvegardées localement.");
        }
        closeModal();
    }

    @FXML
    private void handleDeleteClient() {
        ClientDTO selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected != null && networkManager.isNetworkAvailable()) {
            webSocketClient.deleteClient(selected.getClientId());
            clientTable.getItems().remove(selected);
        } else {
            updateStatus("Veuillez sélectionner un client et vérifier la connexion réseau.");
        }
    }

    @FXML
    private void handleUpdateClient() {
        ClientDTO selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected != null && networkManager.isNetworkAvailable()) {
            if (nameField == null || addressField == null || balanceField == null) {
                updateStatus("Erreur: Les champs du formulaire ne sont pas initialisés.");
                return;
            }
            selected.setName(nameField.getText());
            selected.setAddress(addressField.getText());
            try {
                selected.setBalance(Double.parseDouble(balanceField.getText()));
            } catch (NumberFormatException e) {
                updateStatus("Erreur: Le solde doit être un nombre valide.");
                return;
            }
            webSocketClient.updateClient(selected);
        } else {
            updateStatus("Veuillez sélectionner un client et vérifier la connexion réseau.");
        }
    }

    @FXML
    private void closeModal() {
        if (modalStage != null) {
            modalStage.close();
            clearFields();
        }
    }

    public void updateClientTable(List<ClientDTO> clients) {
        Platform.runLater(() -> {
            ObservableList<ClientDTO> data = FXCollections.observableArrayList(clients);
            clientTable.setItems(data);
        });
    }

    public void updateStatus(String message) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(message);
            } else {
                System.err.println("statusLabel is null, cannot update status: " + message);
            }
        });

        // Check if network status changed from unavailable to available
        if (message.equals("Réseau disponible") && !wasNetworkAvailable) {
            syncPendingClients();
        }
        wasNetworkAvailable = networkManager != null && networkManager.isNetworkAvailable();
    }

    private void syncPendingClients() {
        List<ClientDTO> pendingClients = jsonFileManager.getPendingClients();
        if (!pendingClients.isEmpty()) {
            updateStatus("Synchronisation des clients en attente...");
            for (ClientDTO client : pendingClients) {
                webSocketClient.sendClientData(client);
            }
            // Delete the JSON files after successful sync
            File dir = new File("pending/");
            for (File file : dir.listFiles()) {
                jsonFileManager.deleteJsonFile(file);
            }
            updateStatus("Synchronisation terminée.");
        }
    }

    private void clearFields() {
        if (nameField != null) nameField.clear();
        if (addressField != null) addressField.clear();
        if (balanceField != null) balanceField.clear();
    }
}