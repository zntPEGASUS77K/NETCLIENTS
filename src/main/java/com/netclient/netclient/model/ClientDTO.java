package com.netclient.netclient.model;

import lombok.Data;

@Data
public class ClientDTO {

    private Long clientId;
    private String name;
    private String address;
    private Double balance;

}