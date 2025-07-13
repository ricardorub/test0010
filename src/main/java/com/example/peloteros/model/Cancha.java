package com.example.peloteros.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Cancha {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String tipo; // F5, F7, F11
    private String ubicacion;
    private double precioPorHora;
    private String descripcion;
    private String horarioApertura;  // New field for opening time
    private String horarioCierre;    // New field for closing time
    private String fotoUrl;
}