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
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;

public class ClientController {

    @FXML private TableView<ClientDTO> clientTable;
    @FXML private TableColumn<ClientDTO, Long> idColumn;
    @FXML private TableColumn<ClientDTO, String> nameColumn;
    @FXML private TableColumn<ClientDTO, String> addressColumn;
    @FXML private TableColumn<ClientDTO, Double> balanceColumn;
    @FXML private Label statusLabel;
    @FXML private Circle networkIndicator;
    @FXML private Button deleteButton;

    private TcpClient tcpClient;
    private NetworkManager networkManager;
    private JsonFileManager jsonFileManager;
    private Stage modalStage;
    private boolean isUpdating = false;
    private boolean modalIsOpen = false;
    private List<ClientDTO> localClients = new ArrayList<>();

    // Variable pour gérer le mode de suppression
    private boolean deleteMode = false;
    private Alert confirmationDialog;

    @FXML
    public void initialize() {
        if (clientTable == null || idColumn == null || nameColumn == null ||
                addressColumn == null || balanceColumn == null || statusLabel == null ||
                networkIndicator == null) {
            System.err.println("One or more FXML elements are not properly injected!");
            return;
        }

        tcpClient = new TcpClient(this);
        networkManager = new NetworkManager(this);
        jsonFileManager = new JsonFileManager();

        // Configuration des colonnes
        idColumn.setCellValueFactory(new PropertyValueFactory<>("clientId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        addressColumn.setCellValueFactory(new PropertyValueFactory<>("address"));
        balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balance"));

        // Gestionnaire de sélection amélioré
        clientTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && !isUpdating && !modalIsOpen) {
                if (deleteMode) {
                    // Mode suppression : afficher confirmation immédiatement
                    Platform.runLater(() -> {
                        if (deleteMode && clientTable.getSelectionModel().getSelectedItem() == newSelection) {
                            showDeleteConfirmation(newSelection);
                        }
                    });
                } else {
                    Platform.runLater(() -> {
                        if (!isUpdating && !modalIsOpen && !deleteMode &&
                                clientTable.getSelectionModel().getSelectedItem() == newSelection) {
                            openEditClientModal(newSelection);
                        }
                    });
                }
            }
        });

        networkManager.checkNetworkStatus();
    }

    @FXML
    private void openAddClientModal() {
        if (modalIsOpen) return;
        resetDeleteMode();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/netclient/frontend/add-client-modal.fxml"));
            AddClientController controller = new AddClientController();
            controller.setParentController(this);
            controller.setJsonFileManager(jsonFileManager);
            controller.setNetworkManager(networkManager);
            controller.setTcpClient(tcpClient);
            loader.setController(controller);
            Parent root = loader.load();
            modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Ajouter un Client");
            modalStage.setScene(new Scene(root));
            modalIsOpen = true;
            modalStage.setOnHidden(e -> {
                modalIsOpen = false;
                onModalClosed();
            });
            modalStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Erreur lors de l'ouverture du modal: " + e.getMessage());
            modalIsOpen = false;
        }
    }
    @FXML
    private void openEditClientModal() {
        ClientDTO selected = clientTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            updateStatus("Veuillez sélectionner un client.");
            return;
        }
        resetDeleteMode();
        openEditClientModal(selected);
    }

    private void openEditClientModal(ClientDTO client) {
        if (modalIsOpen) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/netclient/frontend/edit-client-modal.fxml"));
            EditClientController controller = new EditClientController();
            controller.setParentController(this);
            controller.setJsonFileManager(jsonFileManager);
            controller.setNetworkManager(networkManager);
            controller.setTcpClient(tcpClient);
            controller.setSelectedClient(client);
            loader.setController(controller);
            Parent root = loader.load();
            modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setTitle("Modifier un Client");
            modalStage.setScene(new Scene(root));
            modalIsOpen = true;
            modalStage.setOnHidden(e -> {
                modalIsOpen = false;
                onModalClosed();
            });

            modalStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Erreur lors de l'ouverture du modal: " + e.getMessage());
            modalIsOpen = false;
        }
    }

    @FXML
    private void handleDeleteClient() {
        // Étape 1 : Activer le mode suppression
        deleteMode = true;
        clientTable.getSelectionModel().clearSelection();

        // Changer l'apparence du bouton pour indiquer le mode actif
        if (deleteButton != null) {
            deleteButton.setStyle("-fx-background-color: #d32f2f; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 16;");
            deleteButton.setText("Annuler suppression");
        }

        // Afficher le message d'instruction
        updateStatus("Veuillez sélectionner un client à supprimer.");
    }

    private void showDeleteConfirmation(ClientDTO clientToDelete) {
        // Créer le dialog de confirmation
        confirmationDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationDialog.setTitle("Confirmation de suppression");
        confirmationDialog.setHeaderText("Supprimer le client ?");
        confirmationDialog.setContentText("Voulez-vous vraiment supprimer le client " +
                clientToDelete.getName() + " ?");

        // Personnaliser les boutons
        confirmationDialog.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        // Afficher le dialog et traiter la réponse
        Optional<ButtonType> result = confirmationDialog.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Confirmation reçue - procéder à la suppression
            performDelete(clientToDelete);
        }

        // Fermer automatiquement le dialog et réinitialiser le mode
        closeConfirmationDialog();
        resetDeleteMode();
    }

    private void performDelete(ClientDTO clientToDelete) {
        if (networkManager.isNetworkAvailable()) {
            // Réseau disponible : Suppression immédiate
            deleteClientOnline(clientToDelete);
        } else {
            // Réseau indisponible : Sauvegarde locale
            deleteClientOffline(clientToDelete);
        }
    }


    private void deleteClientOnline(ClientDTO clientToDelete) {
        // Send the delete operation to the server
        boolean success = tcpClient.sendOperation("DELETE", clientToDelete);

        if (success) {
            updateStatus("Suppression du client en cours...");

            // Supprimer immédiatement de la liste locale pour mise à jour instantanée
            Platform.runLater(() -> {
                // Supprimer de la liste locale d'abord
                localClients.removeIf(c -> c.getClientId().equals(clientToDelete.getClientId()));

                // Mettre à jour l'affichage immédiatement avec la liste locale modifiée
                updateLocalClientTable();

                // Ensuite récupérer les données du serveur pour synchronisation complète
                List<ClientDTO> updatedClients = tcpClient.fetchAllClientsFromServer();
                if (updatedClients != null) {
                    updateClientTable(updatedClients);
                    updateStatus("Enregistrement supprimé avec succès. Table mise à jour.");
                } else {
                    updateStatus("Client supprimé localement et sur le serveur.");
                }
            });
        } else {
            updateStatus("Échec de la suppression du client sur le serveur.");
        }
    }

    private void deleteClientOffline(ClientDTO clientToDelete) {
        jsonFileManager.saveOperationToJson("DELETE", clientToDelete, this::updateStatus);
        updateStatus("Suppression enregistrée localement");

    }

    private void closeConfirmationDialog() {
        if (confirmationDialog != null) {
            confirmationDialog.close();
            confirmationDialog = null;
        }
    }

    private void resetDeleteMode() {
        deleteMode = false;
        clientTable.getSelectionModel().clearSelection();
        if (deleteButton != null) {
            deleteButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 8 16;");
            deleteButton.setText("Supprimer");
        }
    }
    @FXML
    private void cancelDeleteMode() {
        if (deleteMode) {
            resetDeleteMode();
            updateStatus("Mode suppression annulé.");
        } else {
            handleDeleteClient();
        }
    }

    public void updateClientTable(List<ClientDTO> clients) {
        Platform.runLater(() -> {
            clients.sort((c1, c2) -> Long.compare(c1.getClientId(), c2.getClientId()));
            localClients = new ArrayList<>(clients);
            ObservableList<ClientDTO> data = FXCollections.observableArrayList(clients);
            isUpdating = true;
            clientTable.setItems(data);
            clientTable.getSelectionModel().clearSelection();
            isUpdating = false;
            clientTable.refresh();
        });
    }

    private void updateLocalClientTable() {
        Platform.runLater(() -> {
            localClients.sort((c1, c2) -> Long.compare(c1.getClientId(), c2.getClientId()));
            ObservableList<ClientDTO> data = FXCollections.observableArrayList(localClients);
            isUpdating = true;
            clientTable.setItems(data);
            clientTable.getSelectionModel().clearSelection();
            isUpdating = false;
            clientTable.refresh();
        });
    }

    public void updateClientLocally(ClientDTO updatedClient) {
        Platform.runLater(() -> {
            for (int i = 0; i < localClients.size(); i++) {
                if (localClients.get(i).getClientId().equals(updatedClient.getClientId())) {
                    localClients.set(i, updatedClient);
                    break;
                }
            }
            if (!networkManager.isNetworkAvailable()) {
                updateStatus("Modification sauvegardée localement. L'affichage sera mis à jour lors de la reconnexion.");
            }
        });
    }

    public void updateClientTableAfterEdit(List<ClientDTO> clients) {
        Platform.runLater(() -> {
            if (clients != null && networkManager.isNetworkAvailable()) {
                clients.sort((c1, c2) -> Long.compare(c1.getClientId(), c2.getClientId()));
                localClients = new ArrayList<>(clients);
                ObservableList<ClientDTO> data = FXCollections.observableArrayList(clients);
                isUpdating = true;
                clientTable.setItems(data);
                isUpdating = false;
                clientTable.refresh();
            }
        });
    }

    public void onModalClosed() {
        Platform.runLater(() -> {
            modalIsOpen = false;
            resetDeleteMode();
            if (!isUpdating) {
                clientTable.getSelectionModel().clearSelection();
                clientTable.refresh();
            }
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
    }

    public void updateNetworkIndicator(boolean isAvailable) {
        Platform.runLater(() -> {
            if (networkIndicator != null) {
                networkIndicator.setFill(isAvailable ? Color.GREEN : Color.RED);
                statusLabel.setText(isAvailable ? "Réseau : disponible" : "Réseau : indisponible");

                if (isAvailable) {
                    syncPendingClients();
                }
            }
        });
    }

    public void syncPendingClients() {
        if (!networkManager.isNetworkAvailable()) {
            return;
        }

        List<Map<String, Object>> pendingOperations = jsonFileManager.getPendingOperations();
        if (!pendingOperations.isEmpty()) {
            updateStatus("Synchronisation des opérations en attente...");

            for (Map<String, Object> operationData : pendingOperations) {
                String operation = (String) operationData.get("operation");
                ClientDTO client = new com.fasterxml.jackson.databind.ObjectMapper()
                        .convertValue(operationData.get("client"), ClientDTO.class);

                boolean success = tcpClient.sendOperation(operation, client);

                if (success && "DELETE".equals(operation)) {
                    localClients.removeIf(c -> c.getClientId().equals(client.getClientId()));
                }
            }
            List<ClientDTO> updatedClients = tcpClient.fetchAllClientsFromServer();
            updateClientTable(updatedClients);
            File dir = new File("pending/");
            if (dir.exists() && dir.listFiles() != null) {
                for (File file : dir.listFiles()) {
                    jsonFileManager.deleteJsonFile(file);
                }
            }

            updateStatus("Synchronisation terminée. Affichage mis à jour.");
        }
    }

    public List<ClientDTO> getLocalClients() {
        return new ArrayList<>(localClients);
    }

    public boolean isDeleteMode() {
        return deleteMode;
    }

    public boolean isModalOpen() {
        return modalIsOpen;
    }
}