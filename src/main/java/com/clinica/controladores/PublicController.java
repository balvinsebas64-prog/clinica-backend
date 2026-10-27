package com.clinica.controladores;

import com.clinica.repositorios.EspecialidadRepository;
import com.clinica.repositorios.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PublicController {

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @GetMapping("/publico")
    public String publico(Model model) {
        model.addAttribute("especialidades", especialidadRepository.findAll());
        model.addAttribute("medicos", medicoRepository.findAll());
        return "publico";
    }
}