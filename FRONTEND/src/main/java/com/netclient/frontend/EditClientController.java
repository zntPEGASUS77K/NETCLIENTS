package com.netclient.frontend;

import com.netclient.frontend.utils.JsonFileManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class EditClientController {

    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField balanceField;

    private ClientController parentController;
    private JsonFileManager jsonFileManager;
    private NetworkManager networkManager;
    private TcpClient tcpClient;
    private ClientDTO selectedClient;

    public void setParentController(ClientController parentController) {
        this.parentController = parentController;
    }

    public void setJsonFileManager(JsonFileManager jsonFileManager) {
        this.jsonFileManager = jsonFileManager;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public void setTcpClient(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    public void setSelectedClient(ClientDTO client) {
        this.selectedClient = client;
        if (client != null && nameField != null && addressField != null && balanceField != null) {
            nameField.setText(client.getName());
            addressField.setText(client.getAddress());
            balanceField.setText(client.getBalance().toString());
        }
    }

    @FXML
    private void initialize() {
        if (selectedClient != null && nameField != null && addressField != null && balanceField != null) {
            nameField.setText(selectedClient.getName());
            addressField.setText(selectedClient.getAddress());
            balanceField.setText(selectedClient.getBalance().toString());
        }
    }

    @FXML
    private void handleUpdateClient() {
        if (nameField == null || addressField == null || balanceField == null) {
            parentController.updateStatus("Erreur: Les champs du formulaire ne sont pas initialisés.");
            return;
        }

        String name = nameField.getText();
        String address = addressField.getText();
        String balanceText = balanceField.getText();

        if (name.isEmpty() || address.isEmpty()) {
            nameField.setStyle("-fx-border-color: red;");
            addressField.setStyle("-fx-border-color: red;");
            parentController.updateStatus("Erreur: Les champs Nom et Adresse sont requis.");
            return;
        } else {
            nameField.setStyle("-fx-border-color: #00695c;");
            addressField.setStyle("-fx-border-color: #00695c;");
        }

        try {
            // Créer une copie du client avec les nouvelles données
            ClientDTO updatedClient = new ClientDTO();
            updatedClient.setClientId(selectedClient.getClientId());
            updatedClient.setName(name);
            updatedClient.setAddress(address);
            updatedClient.setBalance(Double.parseDouble(balanceText));

            if (networkManager.isNetworkAvailable()) {
                // Mode en ligne : envoyer au serveur et mettre à jour l'affichage immédiatement
                if (tcpClient.sendOperation("UPDATE", updatedClient)) {
                    closeModal();
                    Platform.runLater(() -> {
                        parentController.updateClientTableAfterEdit(tcpClient.fetchAllClientsFromServer());
                    });
                } else {
                    parentController.updateStatus("Échec de la modification. Veuillez réessayer.");
                }
            } else {
                // Mode hors ligne : sauvegarder localement mais NE PAS mettre à jour l'affichage
                jsonFileManager.saveOperationToJson("UPDATE", updatedClient, parentController::updateStatus);

                // Mettre à jour les données locales sans affecter l'affichage
                parentController.updateClientLocally(updatedClient);

                parentController.updateStatus("Réseau indisponible. Modification sauvegardée localement.");
                networkManager.forceOffline();
                closeModal();
            }
        } catch (NumberFormatException e) {
            balanceField.setStyle("-fx-border-color: red;");
            parentController.updateStatus("Erreur: Le solde doit être un nombre valide.");
            return;
        }
    }

    @FXML
    private void closeModal() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
        clearFields();
        if (parentController != null) {
            Platform.runLater(() -> parentController.onModalClosed());
        }
    }

    private void clearFields() {
        if (nameField != null) {
            nameField.clear();
            nameField.setStyle("-fx-border-color: #00695c;");
        }
        if (addressField != null) {
            addressField.clear();
            addressField.setStyle("-fx-border-color: #00695c;");
        }
        if (balanceField != null) {
            balanceField.clear();
            balanceField.setStyle("-fx-border-color: #00695c;");
        }
    }
}