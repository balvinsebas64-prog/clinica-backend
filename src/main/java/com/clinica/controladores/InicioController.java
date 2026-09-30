package com.clinica.controladores;

import com.clinica.repositorios.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @Autowired
    private PacienteRepository pacienteRepository;

    @GetMapping("/")
    public String inicio(Model model) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        return "inicio";
    }
}