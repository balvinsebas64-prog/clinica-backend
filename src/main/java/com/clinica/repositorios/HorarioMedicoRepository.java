package com.clinica.repositorios;

import com.clinica.entidades.DiaSemana;
import com.clinica.entidades.HorarioMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioMedicoRepository extends JpaRepository<HorarioMedico, Integer> {

    List<HorarioMedico> findByMedicoIdOrderByDiaSemanaAsc(Integer medicoId);

    Optional<HorarioMedico> findByMedicoIdAndDiaSemana(Integer medicoId, DiaSemana diaSemana);

    void deleteByMedicoId(Integer medicoId);
}