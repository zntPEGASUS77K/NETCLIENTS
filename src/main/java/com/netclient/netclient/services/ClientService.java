package com.netclient.netclient.services;

import com.netclient.netclient.config.TcpServer;
import com.netclient.netclient.entity.Client;
import com.netclient.netclient.exception.ResourcesNotFoundException;
import com.netclient.netclient.model.ClientDTO;
import com.netclient.netclient.repository.ClientRepository;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @PostConstruct
    public void init() {
        new TcpServer(this);
    }

    public ClientDTO createClient(ClientDTO clientDTO) {
        Client client = mapToEntity(clientDTO);
        Client savedClient = clientRepository.save(client);
        return mapToDTO(savedClient);
    }

    public List<ClientDTO> getAllClients() {
        return clientRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public ClientDTO getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Client not found with id: " + id));
        return mapToDTO(client);
    }

    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Client not found with id: " + id));

        client.setName(clientDTO.getName());
        client.setAddress(clientDTO.getAddress());
        client.setBalance(clientDTO.getBalance());

        Client updatedClient = clientRepository.save(client);
        return mapToDTO(updatedClient);
    }

    public void deleteClient(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourcesNotFoundException("Client not found with id: " + id));
        clientRepository.delete(client);
    }

    private ClientDTO mapToDTO(Client client) {
        ClientDTO dto = new ClientDTO();
        dto.setClientId(client.getClientId());
        dto.setName(client.getName());
        dto.setAddress(client.getAddress());
        dto.setBalance(client.getBalance());
        return dto;
    }

    private Client mapToEntity(ClientDTO dto) {
        Client client = new Client();
        client.setClientId(dto.getClientId());
        client.setName(dto.getName());
        client.setAddress(dto.getAddress());
        client.setBalance(dto.getBalance());
        return client;
    }
}