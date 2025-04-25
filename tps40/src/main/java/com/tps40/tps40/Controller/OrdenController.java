package com.tps40.tps40.Controller;

import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Service.OrdenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/ordenes")
public class OrdenController {

    @Autowired
    private OrdenService ordenService;

    // Listar todas las órdenes para administración
    @GetMapping
    public String listarOrdenes(Model model) {
        try {
            List<Orden> ordenes = ordenService.obtenerTodasLasOrdenes();
            model.addAttribute("ordenes", ordenes);
            return "admin/ordenes_admin";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las órdenes: " + e.getMessage());
            return "error";
        }
    }

    // Filtrar órdenes por estado
    @GetMapping("/filtrar")
    public String filtrarOrdenes(@RequestParam(required = false) String estado, 
                                @RequestParam(required = false) String fechaDesde,
                                @RequestParam(required = false) String fechaHasta,
                                Model model) {
        try {
            List<Orden> ordenes;
            
            if (estado != null && !estado.isEmpty()) {
                Orden.EstadoOrden estadoOrden = Orden.EstadoOrden.fromString(estado);
                ordenes = ordenService.obtenerOrdenesPorEstado(estadoOrden);
            } else {
                ordenes = ordenService.obtenerTodasLasOrdenes();
            }
            
            // Aquí se podría implementar filtrado por fechas
            
            model.addAttribute("ordenes", ordenes);
            return "admin/ordenes_admin";
        } catch (Exception e) {
            model.addAttribute("error", "Error al filtrar las órdenes: " + e.getMessage());
            return "admin/ordenes_admin";
        }
    }

    // Ver detalles de una orden específica
    @GetMapping("/detalles/{id}")
    public String verDetalleOrden(@PathVariable Long id, Model model) {
        try {
            Orden orden = ordenService.obtenerOrdenPorId(id);
            model.addAttribute("orden", orden);
            return "admin/detalles_orden_admin";
        } catch (Exception e) {
            model.addAttribute("error", "Orden no encontrada: " + e.getMessage());
            return "redirect:/admin/ordenes";
        }
    }

    // Actualizar estado de una orden
    @PostMapping("/actualizar/{id}")
    public String actualizarEstadoOrden(@PathVariable Long id, 
                                       @RequestParam String estado,
                                       RedirectAttributes redirectAttributes) {
        try {
            Orden.EstadoOrden nuevoEstado = Orden.EstadoOrden.fromString(estado);
            ordenService.actualizarEstadoOrden(id, nuevoEstado);
            redirectAttributes.addFlashAttribute("success", "Estado de la orden actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar el estado: " + e.getMessage());
        }
        return "redirect:/admin/ordenes";
    }

    // Eliminar una orden
    @PostMapping("/eliminar/{id}")
    public String eliminarOrden(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ordenService.eliminarOrden(id);
            redirectAttributes.addFlashAttribute("success", "Orden eliminada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la orden: " + e.getMessage());
        }
        return "redirect:/admin/ordenes";
    }
}