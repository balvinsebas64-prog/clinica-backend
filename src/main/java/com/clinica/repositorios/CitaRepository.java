package com.clinica.repositorios;

import com.clinica.entidades.Cita;
import com.clinica.entidades.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByPacienteId(Integer pacienteId);

    List<Cita> findByMedicoId(Integer medicoId);

    // Pacientes únicos atendidos por un médico
    @Query("""
        SELECT DISTINCT c.paciente 
        FROM Cita c 
        WHERE c.medico.id = :medicoId
    """)
    List<Paciente> findPacientesByMedicoId(@Param("medicoId") Integer medicoId);

    // ⭐ NUEVO: Buscar citas en un rango de fechas (útil para reportes diarios o semanales)
    List<Cita> findByFechaHoraBetween(LocalDateTime inicio, LocalDateTime fin);
}