package com.tps40.tps40.Service;

import com.tps40.tps40.Entities.Orden;
import com.tps40.tps40.Entities.Orden.EstadoOrden;
import com.tps40.tps40.Repository.IOrdenRepository;
import com.tps40.tps40.Repository.IProductosRepository;
import com.tps40.tps40.Repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final IOrdenRepository ordenRepository;
    private final IProductosRepository productoRepository;
    private final IUsuarioRepository usuarioRepository;

    @Autowired
    public DashboardService(IOrdenRepository ordenRepository,
                          IProductosRepository productoRepository,
                          IUsuarioRepository usuarioRepository) {
        this.ordenRepository = ordenRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public BigDecimal calcularVentasDelDia() {
        try {
            LocalDateTime inicioDia = LocalDate.now().atStartOfDay();
            LocalDateTime finDia = inicioDia.plusDays(1);
            BigDecimal ventas = ordenRepository.sumVentasEntregadasEntreFechas(inicioDia, finDia);
            return ventas != null ? ventas : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    public long contarOrdenesPendientes() {
        return ordenRepository.countByEstado(EstadoOrden.PENDIENTE);
    }

    public long contarOrdenesPorEstado(EstadoOrden estado) {
        return ordenRepository.countByEstado(estado);
    }

    public long contarTotalProductos() {
        return productoRepository.count();
    }

    public long contarTotalUsuarios() {
        return usuarioRepository.count();
    }

    public Map<String, BigDecimal> obtenerVentasMensuales() {
        Map<String, BigDecimal> ventas = new LinkedHashMap<>();
        LocalDate ahora = LocalDate.now();
        
        try {
            // Intenta obtener datos reales
            for (Map.Entry<String, BigDecimal> entry : ventas.entrySet()) {
                LocalDate mes = ahora.minusMonths(5 - ventas.size());
                BigDecimal total = ordenRepository.sumVentasEntregadasByMonthAndYear(
                    mes.getYear(), mes.getMonthValue());
                if (total != null) {
                    entry.setValue(total);
                }
            }
        } catch (Exception e) {
            // Si hay error, mantiene los ceros
        }
        
        return ventas;
    }
    

    public List<Orden> obtenerUltimasOrdenes(int cantidad) {
        return ordenRepository.findAllByOrderByFechaCreacionDesc(PageRequest.of(0, cantidad)).getContent();
    }

    public List<Orden> obtenerOrdenesPorEstado(EstadoOrden estado, int cantidad) {
        return ordenRepository.findByEstadoOrderByFechaCreacionDesc(estado, PageRequest.of(0, cantidad)).getContent();
    }
}