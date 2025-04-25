package com.tps40.tps40.Controller;

import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.Orden.EstadoOrden;
import com.tps40.tps40.Service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;

    @Autowired
    public AdminController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
public String mostrarDashboard(Model model) {
    model.addAttribute("ventasHoy", dashboardService.calcularVentasDelDia());
    model.addAttribute("ordenesPendientes", dashboardService.contarOrdenesPendientes());
    model.addAttribute("totalProductos", dashboardService.contarTotalProductos());
    model.addAttribute("ventasMensuales", dashboardService.obtenerVentasMensuales());
    model.addAttribute("totalUsuarios", dashboardService.contarTotalUsuarios());
    
    // Obtener últimas órdenes (máximo 5)
    List<Orden> ultimasOrdenes = dashboardService.obtenerUltimasOrdenes(5);
    model.addAttribute("ultimasOrdenes", ultimasOrdenes);
    model.addAttribute("hayOrdenes", !ultimasOrdenes.isEmpty());
    
    return "admin/dashboard";
}


    @GetMapping("/dashboard/filtrar")
    public String filtrarPorEstado(
            @RequestParam("estado") String estadoStr,
            Model model) {
        try {
            EstadoOrden estado = EstadoOrden.fromString(estadoStr);
            model.addAttribute("ordenesFiltradas", dashboardService.obtenerOrdenesPorEstado(estado, 10));
            model.addAttribute("estadoSeleccionado", estado);
            return "admin/partials/ordenes-table";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", "Estado no válido");
            return "admin/partials/error";
        }
    }
}
