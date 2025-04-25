package com.tps40.tps40.Entities;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "carrito_items")

public class CarritoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.EAGER)  // Cambiar de LAZY a EAGER
    @JoinColumn(name = "producto_id", nullable = false)
    private Productos producto;
    
    @Column(name = "cantidad", nullable = false)
    private Integer cantidad = 0;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}
