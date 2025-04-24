package com.tps40.tps40.Controller;
import com.tps40.tps40.Entities.Carrito;
import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.Usuarios;
import com.tps40.tps40.Service.CarritoService;
import com.tps40.tps40.Service.OrdenService;
import com.tps40.tps40.Service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrdenController {
    @Autowired
    private OrdenService ordenService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private CarritoService carritoService;

    // Crear orden desde carrito (POST desde vista de carrito)
    @PostMapping("/crear")
    public String crearOrden(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String direccionEnvio,
            RedirectAttributes redirectAttributes) {
        try {
            Usuarios usuario = usuarioService.obtenerUsuarioPorNombreUsuario(userDetails.getUsername());
            Carrito carrito = carritoService.obtenerCarritoActivo(usuario.getId());
            Orden orden = ordenService.crearOrdenDesdeCarrito(carrito.getId(), direccionEnvio);
            if (carrito == null) {
                redirectAttributes.addFlashAttribute("error", "No tienes un carrito activo");
                return "redirect:/carrito";
            }
            return "redirect:/ordenes_usuarios/" + orden.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    // Ver detalle de orden
    @GetMapping("/{ordenId}")
    public String verOrden(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long ordenId,
            Model model) {
        try {
            Orden orden = ordenService.obtenerOrdenConItems(ordenId);
            model.addAttribute("orden", orden);
            return "ordenes/detalle";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error";
        }
    }

    // Listar órdenes del usuario
    @GetMapping("/usuario/ordenes")
    public String listarOrdenesUsuario(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Usuarios usuario = usuarioService.obtenerUsuarioPorNombreUsuario(userDetails.getUsername());
        model.addAttribute("ordenes", ordenService.listarOrdenesPorUsuario(usuario.getId()));
        model.addAttribute("usuario", usuario);
        return "ordenes_usuarios";
    }

    @PostMapping("/ordenes/{ordenId}/cancelar")
    @PreAuthorize("hasRole('USER')")
    public String cancelarOrden(@PathVariable Long ordenId) {
        ordenService.actualizarEstadoOrden(ordenId, Orden.EstadoOrden.CANCELADO);
        return "redirect:/usuario/ordenes";
    }

    // Actualizar estado (solo ADMIN)
    @PostMapping("/{ordenId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public String actualizarEstado(
            @PathVariable Long ordenId,
            @RequestParam Orden.EstadoOrden nuevoEstado,
            RedirectAttributes redirectAttributes) {
        try {
            ordenService.actualizarEstadoOrden(ordenId, nuevoEstado);
            redirectAttributes.addFlashAttribute("success", "Estado actualizado");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/ordenes/" + ordenId;
    }
    @GetMapping("/admin/ordenes")
    public String mostrarordenesAdmin(){
        return "admin/ordenes_admin"; // Ruta completa a la plantilla
    }

}
