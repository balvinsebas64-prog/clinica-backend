package com.clinica.controladores;

import com.clinica.entidades.HistoriaClinica;
import com.clinica.entidades.Medico;
import com.clinica.entidades.Paciente;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Controller
public class PanelController {

    @Autowired
    private PacienteRepository pacienteRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private MedicoRepository medicoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EspecialidadRepository especialidadRepository;

    @Autowired
    private HistoriaClinicaRepository historiaClinicaRepository;

    // ==========================================
    // PANEL ADMIN
    // ==========================================
    @GetMapping("/admin")
    public String panelAdmin(Model model) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("totalMedicos", medicoRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("citas", citaRepository.findAll());
        return "admin/panel";
    }

    // ==========================================
    // PANEL DOCTOR
    // ==========================================
    @GetMapping("/doctor")
    public String panelDoctor(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        Medico medico = null;
        if (usuario != null) {
            medico = medicoRepository.findByUsuarioId(usuario.getId()).orElse(null);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("medico", medico);
        model.addAttribute("citas", citaRepository.findAll());
        model.addAttribute("pacientes", pacienteRepository.findAll());
        return "doctor/panel";
    }

    // ==========================================
    // PANEL PACIENTE
    // ==========================================
    @GetMapping("/paciente")
    public String panelPaciente(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        Paciente paciente = null;
        List<com.clinica.entidades.Cita> misCitas = new ArrayList<>();

        if (usuario != null && usuario.getEmail() != null) {
            paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);

            if (paciente != null) {
                misCitas = citaRepository.findByPacienteId(paciente.getId());
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("paciente", paciente);
        model.addAttribute("citas", misCitas);
        return "paciente/panel";
    }

    // ==========================================
    // PANEL PERSONAL
    // ==========================================
    @GetMapping("/personal")
    public String panelPersonal(Model model) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("citas", citaRepository.findAll());
        model.addAttribute("pacientes", pacienteRepository.findAll());
        return "personal/panel";
    }

    // ==========================================
    // MÓDULOS EN CONSTRUCCIÓN
    // ==========================================
    @GetMapping("/citas")
    public String citas() { return "citas/listar"; }

    @GetMapping("/historias")
    public String historias() { return "historias/listar"; }

    @GetMapping("/reportes")
    public String reportes() { return "reportes/listar"; }

    // ==========================================
    // RF3: PROGRAMACIÓN DE CITAS
    // ==========================================
    @GetMapping("/citas/programar")
    public String citasProgramar(Model model) {
        model.addAttribute("pacientes", pacienteRepository.findAll());
        model.addAttribute("medicos", medicoRepository.findAll());
        model.addAttribute("especialidades", especialidadRepository.findAll());
        return "citas/programar";
    }

    // ==========================================
    // RF4: REPROGRAMACIÓN
    // ==========================================
    @GetMapping("/citas/reprogramar")
    public String citasReprogramar(Model model) {
        model.addAttribute("citas", citaRepository.findAll());
        return "citas/reprogramar";
    }

    // ==========================================
    // RF5: CANCELACIÓN
    // ==========================================
    @GetMapping("/citas/cancelar")
    public String citasCancelar(Model model) {
        model.addAttribute("citas", citaRepository.findAll());
        return "citas/cancelar";
    }

    // ==========================================
    // RF6: GESTIÓN HISTORIAS
    // ==========================================
    @GetMapping("/historias/gestion")
    public String historiasGestion(Model model) {
        model.addAttribute("pacientes", pacienteRepository.findAll());
        return "historias/gestion";
    }

    // ==========================================
    // RF7: CONSULTA HISTORIAS (personalizado por rol)
    // ==========================================
    @GetMapping("/historias/consulta")
    public String historiasConsulta(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());

        boolean esPaciente = roles.contains("ROLE_PACIENTE");

        if (esPaciente && usuario != null) {
            // Solo mostrar SU historia
            Paciente paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);
            List<Paciente> misPacientes = new ArrayList<>();
            if (paciente != null) {
                misPacientes.add(paciente);
            }
            model.addAttribute("pacientes", misPacientes);
            model.addAttribute("esPaciente", true);
        } else {
            // Médicos y admin ven todas
            model.addAttribute("pacientes", pacienteRepository.findAll());
            model.addAttribute("esPaciente", false);
        }

        return "historias/consulta";
    }

    // ==========================================
    // RF10: REPORTES
    // ==========================================
    @GetMapping("/reportes/generar")
    public String reportesGenerar(Model model) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("totalMedicos", medicoRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        return "reportes/generar";
    }

    // ==========================================
    // RF11: DISPONIBILIDAD
    // ==========================================
    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model) {
        model.addAttribute("medicos", medicoRepository.findAll());
        return "disponibilidad/listar";
    }

    // ==========================================
    // RF12: NOTIFICACIONES
    // ==========================================
    @GetMapping("/notificaciones")
    public String notificaciones(Model model) {
        model.addAttribute("citas", citaRepository.findAll());
        return "notificaciones/listar";
    }
}