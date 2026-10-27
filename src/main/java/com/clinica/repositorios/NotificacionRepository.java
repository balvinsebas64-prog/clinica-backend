package com.clinica.repositorios;

import com.clinica.entidades.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Integer> {
    
    // Para listar en la bandeja (las más nuevas primero)
    List<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Integer usuarioId);
    
    // Para el contador rojo del sidebar
    Long countByUsuarioIdAndLeidaFalse(Integer usuarioId);
}