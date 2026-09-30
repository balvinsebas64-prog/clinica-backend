package com.clinica.controladores;

import com.clinica.entidades.Paciente;
import com.clinica.repositorios.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteRepository repo;

    // Listar todos los pacientes
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pacientes", repo.findAll());
        return "paciente/listar";
    }

    // Mostrar formulario de nuevo paciente
    @GetMapping("/crear")
    public String crear(Model model) {
        model.addAttribute("paciente", new Paciente());
        model.addAttribute("modoEdicion", false);
        return "paciente/crear";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Paciente paciente = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + id));
        model.addAttribute("paciente", paciente);
        model.addAttribute("modoEdicion", true);
        return "paciente/crear";
    }

    // Guardar paciente (nuevo o editado)
    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Paciente paciente) {
        boolean esNuevo = (paciente.getId() == null);
        repo.save(paciente);
        String mensaje = esNuevo ? "Paciente creado correctamente" : "Paciente actualizado correctamente";
        return "redirect:/pacientes?exito=" + mensaje;
    }

    // Eliminar paciente
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/pacientes?exito=Paciente eliminado correctamente";
    }
}