package com.clinica.controladores;

import com.clinica.entidades.Medico;
import com.clinica.repositorios.EspecialidadRepository;
import com.clinica.repositorios.MedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/medicos")
public class MedicoController {

    @Autowired
    private MedicoRepository repo;

    @Autowired
    private EspecialidadRepository especialidadRepo;

    @GetMapping
    public String listar(@RequestParam(value = "especialidad", required = false) Integer especialidadId,
                          @RequestParam(value = "buscar", required = false) String buscar,
                          Model model) {

        List<Medico> medicos;

        if (especialidadId != null) {
            medicos = repo.findByEspecialidadId(especialidadId);
        } else if (buscar != null && !buscar.isEmpty()) {
            medicos = repo.findByNombreContainingOrApellidoContaining(buscar, buscar);
        } else {
            medicos = repo.findAll();
        }

        model.addAttribute("medicos", medicos);
        model.addAttribute("especialidades", especialidadRepo.findAll());
        model.addAttribute("especialidadSeleccionada", especialidadId);
        model.addAttribute("busqueda", buscar);
        return "medico/listar";
    }

    @GetMapping("/crear")
    public String crear(Model model) {
        model.addAttribute("medico", new Medico());
        model.addAttribute("especialidades", especialidadRepo.findAll());
        model.addAttribute("modoEdicion", false);
        return "medico/crear";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Medico medico = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Médico no encontrado: " + id));
        model.addAttribute("medico", medico);
        model.addAttribute("especialidades", especialidadRepo.findAll());
        model.addAttribute("modoEdicion", true);
        return "medico/crear";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Medico medico) {
        boolean esNuevo = (medico.getId() == null);
        repo.save(medico);
        String mensaje = esNuevo ? "Médico creado correctamente" : "Médico actualizado correctamente";
        return "redirect:/medicos?exito=" + mensaje;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        repo.deleteById(id);
        return "redirect:/medicos?exito=Médico eliminado correctamente";
    }
}