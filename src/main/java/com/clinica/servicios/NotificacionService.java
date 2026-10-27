package com.clinica.servicios;

import com.clinica.entidades.Notificacion;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificacionService {

    @Autowired
    private NotificacionRepository repo;

    // Crear notificación
    public void crear(Usuario usuario, String titulo, String mensaje, String urlDestino) {
        if (usuario == null) return;
        
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTitulo(titulo);
        n.setMensaje(mensaje);
        n.setLeida(false);
        n.setFechaCreacion(LocalDateTime.now());
        n.setUrlDestino(urlDestino);
        repo.save(n);
    }

    // Listar notificaciones
    public List<Notificacion> listar(Integer usuarioId) {
        return repo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
    }

    // Contar no leídas
    public Long contarNoLeidas(Integer usuarioId) {
        return repo.countByUsuarioIdAndLeidaFalse(usuarioId);
    }

    // Marcar como leída
    public void marcarLeida(Integer id) {
        Notificacion n = repo.findById(id).orElse(null);
        if (n != null) {
            n.setLeida(true);
            repo.save(n);
        }
    }

    // Marcar todas como leídas
    public void marcarTodasLeidas(Integer usuarioId) {
        List<Notificacion> lista = repo.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId);
        for (Notificacion n : lista) {
            n.setLeida(true);
        }
        repo.saveAll(lista);
    }
}