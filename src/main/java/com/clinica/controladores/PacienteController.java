package com.clinica.controladores;

import com.clinica.entidades.Establecimiento;
import com.clinica.entidades.Paciente;
import com.clinica.entidades.Seguro;
import com.clinica.repositorios.EstablecimientoRepository;
import com.clinica.repositorios.PacienteRepository;
import com.clinica.repositorios.SeguroRepository;
import com.clinica.servicios.AuditoriaService; // ⭐ IMPORT AGREGADO
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/pacientes")
public class PacienteController {

    @Autowired
    private PacienteRepository repo;

    @Autowired
    private SeguroRepository seguroRepo;

    @Autowired
    private EstablecimientoRepository establecimientoRepo;

    // ⭐ AGREGADO AUDITORÍA
    @Autowired
    private AuditoriaService auditoriaService;

    // Listar todos los pacientes
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pacientes", repo.findAll());
        return "pacientes/listar";
    }

    // Mostrar formulario de nuevo paciente
    @GetMapping("/crear")
    public String crear(Model model) {
        model.addAttribute("paciente", new Paciente());
        model.addAttribute("modoEdicion", false);
        model.addAttribute("establecimientos", establecimientoRepo.findAll());
        return "pacientes/crear";
    }

    // Mostrar formulario de edición
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Paciente paciente = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Paciente no encontrado: " + id));
        
        Seguro seguro = seguroRepo.findByPacienteId(id).orElse(null);
        
        model.addAttribute("paciente", paciente);
        model.addAttribute("seguro", seguro);
        model.addAttribute("establecimientos", establecimientoRepo.findAll());
        model.addAttribute("modoEdicion", true);
        return "pacientes/crear";
    }

    // Guardar paciente (nuevo o editado)
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Paciente paciente,
            @RequestParam(value = "numeroAfiliacion", required = false) String numeroAfiliacion,
            @RequestParam(value = "establecimientoId", required = false) Integer establecimientoId) {
        
        boolean esNuevo = (paciente.getId() == null);

        paciente = repo.save(paciente);

        if (esNuevo) {
            Seguro seguro = new Seguro();
            seguro.setPaciente(paciente);
            seguro.setEstado(com.clinica.entidades.EstadoSeguro.ACTIVO);
            seguro.setFechaAfiliacion(LocalDate.now());
            seguro.setCuotaMensual(50.0);

            if (numeroAfiliacion != null && !numeroAfiliacion.isEmpty()) {
                seguro.setNumeroAfiliacion(numeroAfiliacion);
            } else {
                seguro.setNumeroAfiliacion("ESS-" + LocalDate.now().getYear() + "-" + paciente.getDni());
            }

            if (establecimientoId != null) {
                Establecimiento est = establecimientoRepo.findById(establecimientoId).orElse(null);
                seguro.setEstablecimiento(est);
            }

            seguroRepo.save(seguro);

            // ⭐ AUDITORÍA
            auditoriaService.registrar("Crear Paciente", "Se creó al paciente " + paciente.getNombre() + " " + paciente.getApellido());
        } else {
            Seguro seguro = seguroRepo.findByPacienteId(paciente.getId()).orElse(null);
            if (seguro != null) {
                if (numeroAfiliacion != null && !numeroAfiliacion.isEmpty()) {
                    seguro.setNumeroAfiliacion(numeroAfiliacion);
                }
                if (establecimientoId != null) {
                    Establecimiento est = establecimientoRepo.findById(establecimientoId).orElse(null);
                    seguro.setEstablecimiento(est);
                }
                seguroRepo.save(seguro);
            }

            // ⭐ AUDITORÍA
            auditoriaService.registrar("Editar Paciente", "Se actualizó al paciente " + paciente.getNombre() + " " + paciente.getApellido());
        }

        String mensaje = esNuevo ? "Paciente creado correctamente con seguro activo" : "Paciente actualizado correctamente";
        return "redirect:/pacientes?exito=" + mensaje;
    }

    // Eliminar paciente
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        Paciente paciente = repo.findById(id).orElse(null);
        repo.deleteById(id);

        // ⭐ AUDITORÍA
        if (paciente != null) {
            auditoriaService.registrar("Eliminar Paciente", "Se eliminó al paciente " + paciente.getNombre() + " " + paciente.getApellido());
        }

        return "redirect:/pacientes?exito=Paciente eliminado correctamente";
    }
}