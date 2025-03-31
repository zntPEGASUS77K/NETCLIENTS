package com.netclient.netclient.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "client")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "n_client")
    private Long clientId;

    @Column(name = "nom", nullable = false)
    private String name;

    @Column(name = "adresse")
    private String address;

    @Column(name = "solde")
    private Double balance;
}
