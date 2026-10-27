package com.clinica.repositorios;

import com.clinica.entidades.PagoSeguro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoSeguroRepository extends JpaRepository<PagoSeguro, Integer> {
    
    // Para el historial
    List<PagoSeguro> findBySeguroIdOrderByMesPagadoDesc(Integer seguroId);

    // ⭐ ESTA ES LA LÍNEA QUE FALTABA (Para evitar el doble pago)
    Optional<PagoSeguro> findBySeguroIdAndMesPagado(Integer seguroId, String mesPagado);
}