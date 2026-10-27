package com.clinica.repositorios;

import com.clinica.entidades.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer> {
    List<Auditoria> findAllByOrderByFechaDesc();
}