package com.clinica.servicios;

import com.clinica.entidades.Auditoria;
import com.clinica.repositorios.AuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditoriaService {

    @Autowired
    private AuditoriaRepository repo;

    // ⭐ Registra la acción automáticamente
    public void registrar(String accion, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null) ? auth.getName() : "SISTEMA";

        Auditoria a = new Auditoria();
        a.setUsuario(usuario);
        a.setAccion(accion);
        a.setDetalle(detalle);
        a.setFecha(LocalDateTime.now());
        repo.save(a);
    }

    public List<Auditoria> listarTodo() {
        return repo.findAllByOrderByFechaDesc();
    }
}