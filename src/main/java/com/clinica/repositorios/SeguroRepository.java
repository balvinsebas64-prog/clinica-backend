package com.clinica.repositorios;

import com.clinica.entidades.Seguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeguroRepository extends JpaRepository<Seguro, Integer> {
    Optional<Seguro> findByPacienteId(Integer pacienteId);
}