package com.clinica.servicios;

import com.clinica.entidades.Configuracion;
import com.clinica.repositorios.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguracionService {

    @Autowired
    private ConfiguracionRepository configuracionRepository;

    public List<Configuracion> listarTodas() {
        return configuracionRepository.findAll();
    }

    public String obtenerValor(String clave) {
        return configuracionRepository.findByClave(clave)
                .map(Configuracion::getValor)
                .orElse(null);
    }

    public Configuracion guardar(Configuracion config) {
        return configuracionRepository.save(config);
    }

    public Configuracion obtenerPorId(Integer id) {
        return configuracionRepository.findById(id).orElse(null);
    }
}