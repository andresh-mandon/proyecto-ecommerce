package com.tps40.tps40.Listener;

import com.tps40.tps40.Events.UsuarioRegistradoEvent;
import com.tps40.tps40.Service.CarritoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CarritoEventListener {
    private final CarritoService carritoService;

    @Autowired
    public CarritoEventListener(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @EventListener
    @Transactional
    public void handleUsuarioRegistradoEvent(UsuarioRegistradoEvent event) {
        carritoService.obtenerCarritoActivo(event.getUsuario().getId());
    }
}
