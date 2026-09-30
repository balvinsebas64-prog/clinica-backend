package com.clinica.repositorios;

import com.clinica.entidades.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Integer> {

    List<Cita> findByPacienteId(Integer pacienteId);

    List<Cita> findByMedicoId(Integer medicoId);
}