package com.tps40.tps40.Events;

import com.tps40.tps40.Entities.Usuarios;
import org.springframework.context.ApplicationEvent;

public class UsuarioRegistradoEvent extends ApplicationEvent {
    private final Usuarios usuario;

    public UsuarioRegistradoEvent(Object source, Usuarios usuario) {
        super(source);
        this.usuario = usuario;
    }

    public Usuarios getUsuario() {
        return usuario;
    }
}
