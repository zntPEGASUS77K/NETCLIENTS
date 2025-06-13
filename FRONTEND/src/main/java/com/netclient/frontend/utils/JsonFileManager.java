package com.netclient.frontend.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netclient.frontend.ClientDTO;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class JsonFileManager {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String PENDING_DIR = "pending/";

    public JsonFileManager() {
        new File(PENDING_DIR).mkdirs();
    }

    public void saveOperationToJson(String operation, ClientDTO client, Consumer<String> errorHandler) {
        try {
            String fileName = PENDING_DIR + operation + "_" + System.currentTimeMillis() + ".json";
            Map<String, Object> data = new HashMap<>();
            data.put("operation", operation);
            data.put("client", client);
            objectMapper.writeValue(new File(fileName), data);
        } catch (Exception e) {
            errorHandler.accept("Erreur lors de la sauvegarde JSON : " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getPendingOperations() {
        List<Map<String, Object>> pending = new ArrayList<>();
        File dir = new File(PENDING_DIR);
        for (File file : dir.listFiles()) {
            try {
                Map<String, Object> data = objectMapper.readValue(file, Map.class);
                pending.add(data);
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