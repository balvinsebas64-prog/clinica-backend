package com.clinica.repositorios;

import com.clinica.entidades.HistoriaClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HistoriaClinicaRepository extends JpaRepository<HistoriaClinica, Integer> {
    Optional<HistoriaClinica> findByPacienteId(Integer pacienteId);
}