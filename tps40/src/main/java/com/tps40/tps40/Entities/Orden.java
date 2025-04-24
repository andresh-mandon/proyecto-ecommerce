package com.tps40.tps40.Entities;
import jakarta.persistence.*;
import lombok.*;
import java.math.*;
import java.time.*;
import java.util.*;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "ordenes")
public class Orden {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuarios usuario;

    @OneToOne(fetch = FetchType.LAZY)  // Relación con Carrito
    @JoinColumn(name = "carrito_id", nullable = false, unique = true)  // Unique evita duplicados
    private Carrito carrito;  // Carrito que generó esta orden

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;


    @Column(name = "direccion_envio", nullable = false)
    private String direccionEnvio;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrdenItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrden estado;
    
    public enum EstadoOrden {
        PENDIENTE, ENVIADO, ENTREGADO, CANCELADO;
        
        // Conversión segura desde String
        public static EstadoOrden fromString(String value) {
            try {
                return EstadoOrden.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado no válido: " + value);
            }
        }
    }
}
