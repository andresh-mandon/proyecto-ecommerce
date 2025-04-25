package com.tps40.tps40.Entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "roles")
public class Rol implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_rol", nullable = false, unique = true)
    private String nombreRol;

    public enum NombreRol {
        USUARIO, ADMIN
    }

    public Rol(NombreRol nombreRol) {
        this.nombreRol = nombreRol.name();
    }
    @OneToMany(mappedBy = "rol", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Usuarios> usuarios;

}