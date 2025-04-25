package com.tps40.tps40.Controller;

import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Service.OrdenService;
import com.tps40.tps40.Service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ordenes")
public class OrdenUsuarioController {

    private static final Logger log = LoggerFactory.getLogger(OrdenUsuarioController.class);

    @Autowired
    private OrdenService ordenService;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/mis-ordenes")
    public String misOrdenes(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "mis-ordenes"; // O a donde corresponda si no está logueado
        }
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
            List<Orden> ordenes = ordenService.obtenerOrdenesPorUsuario(usuario);
            model.addAttribute("ordenes", ordenes);
            log.info("Mostrando mis órdenes para usuario: {}", userDetails.getUsername());
            return "mis_ordenes"; 
        } catch (Exception e) {
            log.error("Error al cargar mis órdenes para usuario: {}", userDetails.getUsername(), e);
            model.addAttribute("error", "Error al cargar tus órdenes: " + e.getMessage());
            return "error"; // O una página de error más específica
        }
    }

    @GetMapping("/{id}")
    public String detalleOrden(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(userDetails.getUsername());
            Orden orden = ordenService.obtenerOrdenPorId(id);

            if (!orden.getUsuario().getId().equals(usuario.getId())) {
                log.warn("Intento de acceso no autorizado a orden ID: {} por usuario: {}", id, userDetails.getUsername());
                return "redirect:/ordenes/mis-ordenes?error=No autorizado";
            }

            model.addAttribute("orden", orden);
             log.info("Mostrando detalle de orden ID: {} para usuario: {}", id, userDetails.getUsername());
            return "ordenes/detalle_orden"; 
        } catch (RuntimeException e) {
             log.error("Error al cargar detalle de orden ID: {} para usuario: {}", id, userDetails.getUsername(), e);
             // Si es Orden no encontrada, redirigir a mis órdenes con mensaje
             if (e.getMessage().contains("Orden no encontrada")) {
                  return "redirect:/ordenes/mis-ordenes?error=Orden no encontrada";
             }
             model.addAttribute("error", "Error al cargar la orden: " + e.getMessage());
             return "error";
        } catch (Exception e) {
             log.error("Error inesperado al cargar detalle de orden ID: {} para usuario: {}", id, userDetails.getUsername(), e);
             model.addAttribute("error", "Error inesperado al cargar la orden.");
             return "error";
        }
    }

    @PostMapping("/crear")
    @ResponseBody // Asegura que la respuesta sea el cuerpo HTTP, no una vista
    public ResponseEntity<?> crearOrden(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
             log.warn("Intento de crear orden sin autenticación.");
            // Devolver 401 Unauthorized en lugar de 500
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "success", false,
                "message", "Debe iniciar sesión para crear una orden."
            ));
        }
        
        String username = userDetails.getUsername();
        log.info("Recibida solicitud POST /ordenes/crear para usuario: {}", username);
        
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorEmail(username);
             log.debug("Usuario {} encontrado, ID: {}", username, usuario.getId());

            // Pasar la dirección del usuario o permitir que se establezca después
            String direccionEnvio = usuario.getDireccion(); 
            log.debug("Dirección de envío para la orden: {}", direccionEnvio);

            Orden orden = ordenService.crearOrdenDesdeCarrito(usuario.getId(), direccionEnvio);
            log.info("Orden ID: {} creada exitosamente para usuario: {}", orden.getId(), username);

            return ResponseEntity.ok().body(Map.of(
                "success", true,
                "message", "Orden creada exitosamente.",
                "ordenId", orden.getId()
            ));
        } catch (RuntimeException e) {
            // Captura excepciones específicas si es necesario (ej. StockInsuficienteException)
            log.error("Error al crear orden para usuario {}: {}", username, e.getMessage(), e);
             // Devolver un mensaje de error más útil al cliente
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of( // Usar BAD_REQUEST si es error de lógica de negocio
                "success", false,
                "message", "Error al crear la orden: " + e.getMessage() // Devolver el mensaje de la excepción
            ));
        } catch (Exception e) {
            log.error("Error inesperado al crear orden para usuario {}: {}", username, e.getMessage(), e);
             // Error genérico para problemas no esperados
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "success", false,
                "message", "Ocurrió un error inesperado al procesar tu pedido."
            ));
        }
    }
}