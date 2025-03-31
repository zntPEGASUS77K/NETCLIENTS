package com.netclient.frontend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netclient.frontend.ClientDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JsonFileManager {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PENDING_DIR = "pending/";

    public JsonFileManager() {
        new File(PENDING_DIR).mkdirs();
    }

    public void saveToJson(ClientDTO client) {
        try {
            String fileName = PENDING_DIR + "client_" + System.currentTimeMillis() + ".json";
            objectMapper.writeValue(new File(fileName), client);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<ClientDTO> getPendingClients() {
        List<ClientDTO> pending = new ArrayList<>();
        File dir = new File(PENDING_DIR);
        for (File file : dir.listFiles()) {
            try {
                pending.add(objectMapper.readValue(file, ClientDTO.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return pending;
    }

    public void deleteJsonFile(File file) {
        file.delete();
    }
}