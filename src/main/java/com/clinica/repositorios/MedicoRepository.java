package com.clinica.repositorios;

import com.clinica.entidades.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Integer> {

    List<Medico> findByEspecialidadId(Integer especialidadId);

    List<Medico> findByNombreContainingOrApellidoContaining(String nombre, String apellido);

    Optional<Medico> findByUsuarioId(Integer usuarioId);

    // Médicos SIN usuario asignado (para el combo de crear usuarios)
    List<Medico> findByUsuarioIsNull();

    // Médicos CON usuario asignado
    List<Medico> findByUsuarioIsNotNull();
}