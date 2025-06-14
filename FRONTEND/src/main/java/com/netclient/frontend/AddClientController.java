package com.netclient.frontend;

import com.netclient.frontend.utils.JsonFileManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddClientController {
    @FXML private TextField nameField;
    @FXML private TextField addressField;
    @FXML private TextField balanceField;

    private ClientController parentController;
    private JsonFileManager jsonFileManager;
    private NetworkManager networkManager;
    private TcpClient tcpClient;

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

    @FXML
    private void handleAddClient() {
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

        ClientDTO client = new ClientDTO();
        client.setName(name);
        client.setAddress(address);
        try {
            client.setBalance(Double.parseDouble(balanceText));
        } catch (NumberFormatException e) {
            balanceField.setStyle("-fx-border-color: red;");
            parentController.updateStatus("Erreur: Le solde doit être un nombre valide.");
            return;
        }

        if (networkManager.isNetworkAvailable()) {
            // Mode en ligne : envoyer au serveur et mettre à jour l'affichage
            if (tcpClient.sendOperation("ADD", client)) {
                // Succès de l'ajout, mettre à jour l'affichage avec les données du serveur
                Platform.runLater(() -> {
                    parentController.updateClientTable(tcpClient.fetchAllClientsFromServer());
                });
                parentController.updateStatus("Client ajouté avec succès.");
            } else {
                parentController.updateStatus("Échec de l'ajout. Veuillez réessayer.");
                return; // Ne pas fermer le modal en cas d'échec
            }
        } else {
            // Mode hors ligne : sauvegarder localement
            jsonFileManager.saveOperationToJson("ADD", client, parentController::updateStatus);
            parentController.updateStatus("Réseau indisponible. Données sauvegardées localement.");
            networkManager.forceOffline();
        }
        closeModal();
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