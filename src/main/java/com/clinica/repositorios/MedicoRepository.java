package com.clinica.repositorios;

import com.clinica.entidades.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Integer> {

    List<Medico> findByEspecialidadId(Integer especialidadId);

    List<Medico> findByNombreContainingOrApellidoContaining(String nombre, String apellido);

    Optional<Medico> findByUsuarioId(Integer usuarioId);

    List<Medico> findByUsuarioIsNull();

    List<Medico> findByUsuarioIsNotNull();

    // ⭐ RESUMEN: total de citas y total de pacientes únicos por médico
    @Query(value = """
        SELECT 
            m.id                       AS id,
            m.nombre                   AS nombre,
            m.apellido                 AS apellido,
            e.nombre                   AS especialidad,
            e.precio                   AS precio,
            COUNT(DISTINCT c.id)       AS totalCitas,
            COUNT(DISTINCT c.paciente_id) AS totalPacientes
        FROM medicos m
        LEFT JOIN especialidades e ON e.id = m.especialidad_id
        LEFT JOIN citas c          ON c.medico_id = m.id
        GROUP BY m.id, m.nombre, m.apellido, e.nombre, e.precio
        ORDER BY totalCitas DESC, m.apellido ASC
    """, nativeQuery = true)
    List<Object[]> obtenerResumenDoctores();
}