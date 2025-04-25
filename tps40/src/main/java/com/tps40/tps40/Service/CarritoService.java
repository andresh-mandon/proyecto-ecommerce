package com.tps40.tps40.Service;

import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.CarritoItem;
import com.tps40.tps40.Entities.Productos;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Repository.ICarritoItemRepository;
import com.tps40.tps40.Repository.ICarritoRepository;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger; // Importa Logger
import org.slf4j.LoggerFactory; // Importa LoggerFactory

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CarritoService {

    private static final Logger log = LoggerFactory.getLogger(CarritoService.class); // Añadir Logger

    @Autowired
    private ICarritoRepository carritoRepository;

    @Autowired
    private ICarritoItemRepository carritoItemRepository;

    @Autowired
    private ProductosService productoService;

    @Autowired
    private UsuarioService usuarioService;

     @Autowired
    private EntityManager entityManager; // Inyectar EntityManager

    @Transactional
    public Carrito crearCarritoParaUsuario(Long usuarioId) {
        log.info("Intentando crear carrito para usuario ID: {}", usuarioId);
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId)
                .orElseThrow(() -> {
                    log.error("Usuario no encontrado al intentar crear carrito: {}", usuarioId);
                    return new RuntimeException("Usuario no encontrado con ID: " + usuarioId);
                });

        Carrito carrito = new Carrito();
        carrito.setUsuario(usuario);
        carrito.setFechaCreacion(LocalDateTime.now()); // Inicializar fecha y total
        carrito.setTotal(BigDecimal.ZERO);
        Carrito carritoGuardado = carritoRepository.save(carrito);
        log.info("Carrito ID: {} creado para usuario ID: {}", carritoGuardado.getId(), usuarioId);
        return carritoGuardado;
    }

    @Transactional(readOnly = true) // readOnly = true si solo lees
    public Carrito obtenerCarritoPorId(Long carritoId) {
        log.debug("Buscando carrito con ID: {}", carritoId);
        return carritoRepository.findById(carritoId)
                .orElseThrow(() -> {
                    log.warn("Carrito no encontrado con ID: {}", carritoId);
                    return new RuntimeException("Carrito no encontrado con ID: " + carritoId);
                });
    }


    @Transactional
    public Carrito obtenerCarritoActivo(Long usuarioId) {
        log.debug("Obteniendo carrito activo para usuario ID: {}", usuarioId);
        Usuarios usuario = usuarioService.obtenerUsuarioPorId(usuarioId)
                .orElseThrow(() -> {
                     log.error("Usuario no encontrado al buscar carrito activo: {}", usuarioId);
                     return new RuntimeException("Usuario no encontrado con ID: " + usuarioId);
                });

        Optional<Carrito> carritoOpt = carritoRepository.findCarritoActivoByUsuario(usuario); // Asegúrate que esta query exista y funcione

        if (carritoOpt.isPresent()) {
            log.debug("Carrito activo encontrado ID: {} para usuario ID: {}", carritoOpt.get().getId(), usuarioId);
            return carritoOpt.get();
        } else {
            log.info("No se encontró carrito activo para usuario ID: {}. Creando uno nuevo.", usuarioId);
            // Llama al método de creación para mantener la lógica centralizada
            return crearCarritoParaUsuario(usuarioId);
        }
    }

    @Transactional
    public Carrito agregarProducto(Long usuarioId, Long productoId, Integer cantidad) {
        log.info("Agregando producto ID: {} (cantidad: {}) al carrito del usuario ID: {}", productoId, cantidad, usuarioId);
        Carrito carrito = obtenerCarritoActivo(usuarioId);
        Productos producto = productoService.obtenerProductoPorId(productoId);

        log.debug("Producto a agregar: ID={}, Nombre={}, Precio={}, Stock={}",
                  producto.getId(), producto.getNombre(), producto.getPrecio(), producto.getStock());

        if (cantidad <= 0) {
             log.warn("Intento de agregar cantidad no positiva ({}) para producto ID: {}", cantidad, productoId);
             throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }

        // Verificar stock ANTES de buscar el item existente
        int cantidadTotalRequerida;
        Optional<CarritoItem> itemExistenteOpt = carritoItemRepository.findByCarritoAndProducto(carrito, producto);

        if (itemExistenteOpt.isPresent()) {
            cantidadTotalRequerida = itemExistenteOpt.get().getCantidad() + cantidad;
        } else {
            cantidadTotalRequerida = cantidad;
        }

        log.debug("Verificando stock: requerido total {}, disponible {}", cantidadTotalRequerida, producto.getStock());
        if (producto.getStock() < cantidadTotalRequerida) {
            log.error("Stock insuficiente para producto ID: {}. Requerido: {}, Disponible: {}", productoId, cantidadTotalRequerida, producto.getStock());
            throw new RuntimeException("Stock insuficiente para: " + producto.getNombre() + ". Disponible: " + producto.getStock());
        }

        if (itemExistenteOpt.isPresent()) {
            CarritoItem item = itemExistenteOpt.get();
            item.setCantidad(cantidadTotalRequerida); // Actualizar con la nueva cantidad total
            item.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad())));
            carritoItemRepository.save(item);
            log.debug("Item existente ID: {} actualizado. Nueva cantidad: {}", item.getId(), item.getCantidad());
        } else {
            CarritoItem nuevoItem = new CarritoItem();
            nuevoItem.setCarrito(carrito);
            nuevoItem.setProducto(producto);
            nuevoItem.setCantidad(cantidad);
            nuevoItem.setSubtotal(producto.getPrecio().multiply(BigDecimal.valueOf(cantidad)));
            carritoItemRepository.save(nuevoItem);
            log.debug("Nuevo item creado con ID temporal (se asignará al guardar) para producto ID: {}", productoId);
        }

        actualizarTotalCarrito(carrito); // Actualiza el total del carrito
        log.info("Producto ID: {} agregado/actualizado en carrito ID: {}. Nuevo total: {}", productoId, carrito.getId(), carrito.getTotal());
        return carrito;
    }

    @Transactional
    public Carrito actualizarCantidadItem(Long itemId, Integer nuevaCantidad) {
         log.info("Actualizando cantidad del item ID: {} a {}", itemId, nuevaCantidad);
        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> {
                     log.error("Item no encontrado al intentar actualizar cantidad: {}", itemId);
                    return new RuntimeException("Item del carrito no encontrado con ID: " + itemId);
                });

        Carrito carrito = item.getCarrito(); // Obtener carrito antes de posible eliminación

        if (nuevaCantidad <= 0) {
            log.info("Cantidad <= 0, eliminando item ID: {}", itemId);
            carritoItemRepository.delete(item);
        } else {
             log.debug("Verificando stock para actualizar item ID: {}. Requerido: {}, Disponible: {}",
                       itemId, nuevaCantidad, item.getProducto().getStock());
            if (item.getProducto().getStock() < nuevaCantidad) {
                log.error("Stock insuficiente al actualizar item ID: {}. Requerido: {}, Disponible: {}",
                          itemId, nuevaCantidad, item.getProducto().getStock());
                throw new RuntimeException("Stock insuficiente para " + item.getProducto().getNombre() + ". Disponible: " + item.getProducto().getStock());
            }
            item.setCantidad(nuevaCantidad);
            item.setSubtotal(item.getProducto().getPrecio().multiply(BigDecimal.valueOf(nuevaCantidad)));
            carritoItemRepository.save(item);
             log.debug("Item ID: {} actualizado. Nueva cantidad: {}", itemId, nuevaCantidad);
        }

        actualizarTotalCarrito(carrito); // Actualiza el total del carrito asociado
        log.info("Cantidad de item ID: {} actualizada en carrito ID: {}. Nuevo total: {}", itemId, carrito.getId(), carrito.getTotal());
        return carrito;
    }

    @Transactional
    public Carrito eliminarItem(Long itemId) {
        log.info("Eliminando item ID: {}", itemId);
        CarritoItem item = carritoItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.error("Item no encontrado al intentar eliminar: {}", itemId);
                    return new RuntimeException("Item del carrito no encontrado con ID: " + itemId);
                });
        Carrito carrito = item.getCarrito(); // Guardar referencia al carrito
        carritoItemRepository.delete(item);
        log.debug("Item ID: {} eliminado de la base de datos.", itemId);
        actualizarTotalCarrito(carrito); // Actualizar el total del carrito
        log.info("Item ID: {} eliminado del carrito ID: {}. Nuevo total: {}", itemId, carrito.getId(), carrito.getTotal());
        return carrito;
    }

    @Transactional
    public void vaciarCarrito(Long carritoId) {
        log.info("Iniciando vaciado de carrito ID: {}", carritoId);

        // No necesitamos cargar el carrito aquí si vamos a limpiar el contexto después

        // 1. Eliminar los items asociados directamente vía bulk delete.
        log.debug("Ejecutando deleteByCarritoId para carrito ID: {}", carritoId);
        try {
            int deletedCount = carritoItemRepository.deleteByCarritoId(carritoId);
            log.debug("Eliminados {} items del carrito ID {} usando deleteByCarritoId.", deletedCount, carritoId);

            // 2. Forzar la ejecución del delete en la BD (opcional pero puede ayudar)
            // entityManager.flush();
            // log.debug("Flush ejecutado después de deleteByCarritoId.");

            // 3. *** PASO CRUCIAL ***: Limpiar el Persistence Context.
            // Esto desasocia todas las entidades previamente cargadas (incluyendo
            // los CarritoItem fantasma y el Carrito original si se cargó antes).
            entityManager.clear();
            log.debug("Persistence Context limpiado después de la eliminación de items.");

        } catch (Exception e) {
            log.error("Error durante la eliminación de items o limpieza del contexto para carrito ID: {}.", carritoId, e);
            throw new RuntimeException("Error al eliminar items del carrito: " + e.getMessage(), e);
        }

        // 4. Como el contexto está limpio, DEBEMOS volver a cargar el carrito
        // para poder actualizar su total.
        log.debug("Volviendo a cargar el Carrito ID: {} después de limpiar el contexto.", carritoId);
        Carrito carritoActualizado = carritoRepository.findById(carritoId)
                .orElseThrow(() -> {
                    log.error("No se encontró el carrito ID: {} después de limpiar el contexto.", carritoId);
                    // Esto sería muy raro si el delete no falló, pero es una guarda de seguridad.
                    return new RuntimeException("Carrito no encontrado con ID: " + carritoId + " después de vaciar items.");
                });

        // 5. Actualizar el total del carrito (ahora en una instancia "limpia").
        log.debug("Actualizando total del carrito ID: {} a CERO.", carritoId);
        carritoActualizado.setTotal(BigDecimal.ZERO);
        // La colección 'items' en esta instancia recién cargada ya estará vacía
        // porque los items fueron eliminados de la BD. No es necesario clear().

        // 6. Guardar la entidad Carrito actualizada.
        try {
            carritoRepository.save(carritoActualizado);
            log.info("Carrito ID: {} vaciado exitosamente y total actualizado a 0.", carritoId);
        } catch (Exception e) {
             log.error("Error al guardar el carrito ID: {} después de actualizar el total.", carritoId, e);
              throw new RuntimeException("Error al guardar el carrito tras vaciarlo: " + e.getMessage(), e);
        }
    }

    // Obtener items del carrito (con sus productos asociados)
    @Transactional(readOnly = true) // Buena práctica para métodos de solo lectura
    public List<CarritoItem> obtenerItemsDeCarrito(Long carritoId) {
        log.debug("Obteniendo items para carrito ID: {}", carritoId);
        // Asegúrate que findByCarritoIdWithProducto haga un JOIN FETCH o similar
        // para evitar N+1 selects al acceder a item.getProducto() después
        List<CarritoItem> items = carritoItemRepository.findByCarritoIdWithProducto(carritoId);
        log.debug("Encontrados {} items para carrito ID: {}", items.size(), carritoId);
        return items;
    }

    // Método auxiliar para actualizar total. Es privado y llamado internamente.
    // Debe ser @Transactional si es llamado desde métodos no transaccionales,
    // pero si siempre es llamado desde otros métodos @Transactional del mismo servicio,
    // heredará la transacción existente.
    @Transactional // Asegura que la actualización del total sea parte de la transacción
    private void actualizarTotalCarrito(Carrito carrito) {
        log.debug("Actualizando total para carrito ID: {}", carrito.getId());
        // Recalcular desde la base de datos para mayor seguridad,
        // especialmente si hay operaciones concurrentes (aunque menos probable en un carrito de usuario)
        List<CarritoItem> itemsActuales = carritoItemRepository.findByCarrito(carrito); // Usar la query que solo trae items
        BigDecimal total = itemsActuales.stream()
                .map(CarritoItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total calculado para carrito ID {}: {}", carrito.getId(), total);
        carrito.setTotal(total);
        carritoRepository.save(carrito); // Guarda el carrito con el total actualizado
        log.debug("Carrito ID {} guardado con nuevo total: {}", carrito.getId(), carrito.getTotal());
    }

    // Método para guardar explícitamente un carrito (útil si se modifica fuera de otros métodos)
    @Transactional
    public Carrito actualizarCarrito(Carrito carrito) {
        log.info("Guardando explícitamente carrito ID: {}", carrito.getId());
        if (carrito == null) {
             log.error("Intento de guardar un carrito nulo.");
            throw new IllegalArgumentException("El carrito no puede ser nulo.");
        }
        // Podrías añadir validaciones aquí si es necesario
        return carritoRepository.save(carrito);
    }

     // --- Métodos que podrías necesitar en tus Repositories ---

     // En ICarritoRepository:
     // Optional<Carrito> findCarritoActivoByUsuario(Usuarios usuario); // Query JPQL/Nativa necesaria si no es estándar

     // En ICarritoItemRepository:
     // Optional<CarritoItem> findByCarritoAndProducto(Carrito carrito, Productos producto);
     // List<CarritoItem> findByCarrito(Carrito carrito);
     // List<CarritoItem> findByCarritoId(Long carritoId); // Para vaciarCarrito si no usas deleteByCarritoId
     // @Query("SELECT ci FROM CarritoItem ci JOIN FETCH ci.producto WHERE ci.carrito.id = :carritoId")
     // List<CarritoItem> findByCarritoIdWithProducto(@Param("carritoId") Long carritoId); // Para obtenerItemsDeCarrito
     // void deleteByCarrito(Carrito carrito); // Opcional, alternativa a deleteByCarritoId o find+deleteAll
     // @Modifying // Necesario para queries de modificación (DELETE, UPDATE)
     // @Query("DELETE FROM CarritoItem ci WHERE ci.carrito.id = :carritoId")
     // int deleteByCarritoId(@Param("carritoId") Long carritoId); // Para vaciarCarrito eficientemente
}