package com.netclient.netclient.controller;

import com.netclient.netclient.model.ClientDTO;
import com.netclient.netclient.services.ClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/${api.version}/clients")
public class ClientController {

    private final ClientService clientService;

    @Value("${api.version}")
    private String apiVersion;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<String> createClient(@RequestBody ClientDTO clientDTO) {
        clientService.createClient(clientDTO);
        return ResponseEntity.ok("Enregistrement ajouté");
    }

    @GetMapping
    public ResponseEntity<List<ClientDTO>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientDTO> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateClient(@PathVariable Long id, @RequestBody ClientDTO clientDTO) {
        clientService.updateClient(id, clientDTO);
        return ResponseEntity.ok("Enregistrement modifié");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok("Enregistrement supprimé");
    }
}