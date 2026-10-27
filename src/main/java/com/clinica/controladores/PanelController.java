package com.clinica.controladores;

import com.clinica.entidades.DiaSemana;
import com.clinica.entidades.HistoriaClinica;
import com.clinica.entidades.HorarioMedico;
import com.clinica.entidades.Medico;
import com.clinica.entidades.Paciente;
import com.clinica.entidades.Seguro;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.*;
import com.clinica.servicios.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    @Autowired
    private HorarioMedicoRepository horarioMedicoRepository;

    @Autowired
    private SeguroRepository seguroRepository;

    @Autowired
    private NotificacionService notificacionService;

    // ==========================================
    // PANEL ADMIN
    // ==========================================
    @GetMapping("/admin")
    public String panelAdmin(Model model, Authentication auth) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("totalMedicos", medicoRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("citas", citaRepository.findAll());

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));

        // DATOS REALES PARA EL GRÁFICO DE LÍNEAS
        List<String> meses = new ArrayList<>();
        List<Long> citasPorMes = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate mes = hoy.minusMonths(i);
            String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
            meses.add(nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1));

            long citasDelMes = citaRepository.findAll().stream()
                .filter(c -> c.getFechaHora() != null && 
                             c.getFechaHora().getMonth() == mes.getMonth() && 
                             c.getFechaHora().getYear() == mes.getYear())
                .count();
            citasPorMes.add(citasDelMes);
        }
        model.addAttribute("meses", meses);
        model.addAttribute("citasPorMes", citasPorMes);

        // DATOS REALES PARA EL GRÁFICO CIRCULAR
        List<Seguro> seguros = seguroRepository.findAll();
        long activos = seguros.stream().filter(s -> s.getEstado().name().equals("ACTIVO")).count();
        long morosos = seguros.stream().filter(s -> s.getEstado().name().equals("MOROSO")).count();
        long inactivos = seguros.stream().filter(s -> s.getEstado().name().equals("INACTIVO")).count();
        
        model.addAttribute("segurosActivos", activos);
        model.addAttribute("segurosMorosos", morosos);
        model.addAttribute("segurosInactivos", inactivos);

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
        List<com.clinica.entidades.Cita> misCitas = new ArrayList<>();
        List<Paciente> misPacientes = new ArrayList<>();

        if (usuario != null) {
            medico = medicoRepository.findByUsuarioId(usuario.getId()).orElse(null);

            if (medico != null) {
                misCitas = citaRepository.findByMedicoId(medico.getId());
                for (com.clinica.entidades.Cita c : misCitas) {
                    if (c.getPaciente() != null && !misPacientes.contains(c.getPaciente())) {
                        misPacientes.add(c.getPaciente());
                    }
                }
            }
            model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("medico", medico);
        model.addAttribute("citas", misCitas);
        model.addAttribute("pacientes", misPacientes);
        return "doctor/panel";
    }

    // ⭐ DIAGNÓSTICOS DEL DOCTOR
    @GetMapping("/doctor/diagnosticos")
    public String misDiagnosticos(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        Medico medico = null;
        List<com.clinica.entidades.Cita> citasAtendidas = new ArrayList<>();

        if (usuario != null) {
            medico = medicoRepository.findByUsuarioId(usuario.getId()).orElse(null);

            if (medico != null) {
                List<com.clinica.entidades.Cita> misCitas = citaRepository.findByMedicoId(medico.getId());
                for (com.clinica.entidades.Cita c : misCitas) {
                    if (c.getEstado().name().equals("ATENDIDA")) {
                        citasAtendidas.add(c);
                    }
                }
            }
            model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("medico", medico);
        model.addAttribute("citas", citasAtendidas);
        return "doctor/diagnosticos";
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
        List<com.clinica.entidades.Cita> citasAtendidas = new ArrayList<>();
        Seguro seguro = null;

        if (usuario != null) {
            model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));

            if (usuario.getEmail() != null) {
                paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);

                if (paciente != null) {
                    misCitas = citaRepository.findByPacienteId(paciente.getId());
                    seguro = seguroRepository.findByPacienteId(paciente.getId()).orElse(null);

                    for (com.clinica.entidades.Cita c : misCitas) {
                        if (c.getEstado().name().equals("ATENDIDA")) {
                            citasAtendidas.add(c);
                        }
                    }
                }
            }
        }

        HistoriaClinica historia = null;
        if (paciente != null) {
            historia = historiaClinicaRepository.findByPacienteId(paciente.getId()).orElse(null);
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("paciente", paciente);
        model.addAttribute("citas", misCitas);
        model.addAttribute("citasAtendidas", citasAtendidas);
        model.addAttribute("historia", historia);
        model.addAttribute("seguro", seguro);
        return "paciente/panel";
    }

    // ⭐ NUEVO: MIS CITAS (PACIENTE)
    @GetMapping("/paciente/mis-citas")
    public String pacienteMisCitas(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        Paciente paciente = null;
        List<com.clinica.entidades.Cita> misCitas = new ArrayList<>();

        if (usuario != null && usuario.getEmail() != null) {
            paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);
            if (paciente != null) {
                misCitas = citaRepository.findByPacienteId(paciente.getId());
            }
        }

        model.addAttribute("citas", misCitas);
        model.addAttribute("paciente", paciente);
        
        Usuario userLogueado = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(userLogueado != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(userLogueado.getId()));
        
        return "paciente/mis-citas";
    }

    // ⭐ NUEVO: HISTORIA CLÍNICA DEL PACIENTE
    @GetMapping("/paciente/mi-historia")
    public String pacienteMiHistoria(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        Paciente paciente = null;
        HistoriaClinica historia = null;

        if (usuario != null && usuario.getEmail() != null) {
            paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);
            if (paciente != null) {
                historia = historiaClinicaRepository.findByPacienteId(paciente.getId()).orElse(null);
            }
        }

        model.addAttribute("usuario", usuario);
        model.addAttribute("paciente", paciente);
        model.addAttribute("historia", historia);
        
        Usuario userLogueado = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(userLogueado != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(userLogueado.getId()));
        
        return "paciente/mi-historia";
    }

    // ==========================================
    // PANEL PERSONAL
    // ==========================================
    @GetMapping("/personal")
    public String panelPersonal(Model model, Authentication auth) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("citas", citaRepository.findAll());
        model.addAttribute("pacientes", pacienteRepository.findAll());

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));

        return "personal/panel";
    }

    // ==========================================
    // OTROS MÉTODOS
    // ==========================================
    @GetMapping("/personal/doctores-resumen")
    public String doctoresResumen(Model model, Authentication auth) {
        List<Object[]> filas = medicoRepository.obtenerResumenDoctores();
        List<Map<String, Object>> resumen = new ArrayList<>();

        for (Object[] f : filas) {
            Map<String, Object> d = new HashMap<>();
            d.put("id", f[0]);
            d.put("nombre", f[1]);
            d.put("apellido", f[2]);
            d.put("especialidad", f[3] != null ? f[3] : "Sin especialidad");
            d.put("precio", f[4] != null ? f[4] : 0);
            d.put("totalCitas", f[5]);
            d.put("totalPacientes", f[6]);
            resumen.add(d);
        }
        model.addAttribute("resumen", resumen);
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "personal/doctores-resumen";
    }

    @GetMapping("/personal/doctores-resumen/{id}")
    public String doctorDetalle(@PathVariable Integer id, Model model, Authentication auth) {
        Medico medico = medicoRepository.findById(id).orElse(null);
        if (medico == null) return "redirect:/personal/doctores-resumen";
        
        Usuario usuario = medico.getUsuario();
        List<com.clinica.entidades.Cita> citas = citaRepository.findByMedicoId(id);
        List<Paciente> pacientes = citaRepository.findPacientesByMedicoId(id);

        model.addAttribute("medico", medico);
        model.addAttribute("usuario", usuario);
        model.addAttribute("citas", citas);
        model.addAttribute("pacientes", pacientes);

        Usuario userLogueado = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(userLogueado != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(userLogueado.getId()));
        return "personal/doctor-detalle";
    }

    @GetMapping("/doctor/mis-citas")
    public String doctorMisCitas(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        Medico medico = medicoRepository.findByUsuarioId(usuario.getId()).orElse(null);
        List<com.clinica.entidades.Cita> misCitas = new ArrayList<>();
        if (medico != null) misCitas = citaRepository.findByMedicoId(medico.getId());
        
        model.addAttribute("citas", misCitas);
        model.addAttribute("medico", medico);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "doctor/mis-citas";
    }

    @GetMapping("/doctor/mis-pacientes")
    public String doctorMisPacientes(Model model, Authentication auth) {
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        Medico medico = medicoRepository.findByUsuarioId(usuario.getId()).orElse(null);
        List<Paciente> misPacientes = new ArrayList<>();
        if (medico != null) {
            List<com.clinica.entidades.Cita> misCitas = citaRepository.findByMedicoId(medico.getId());
            for (com.clinica.entidades.Cita c : misCitas) {
                if (c.getPaciente() != null && !misPacientes.contains(c.getPaciente())) misPacientes.add(c.getPaciente());
            }
        }
        
        model.addAttribute("pacientes", misPacientes);
        model.addAttribute("medico", medico);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "doctor/mis-pacientes";
    }

    @GetMapping("/disponibilidad")
    public String disponibilidad(Model model, Authentication auth) {
        List<Medico> medicos = medicoRepository.findAll();
        Map<Integer, Map<String, HorarioMedico>> horariosPorMedico = new HashMap<>();
        for (Medico m : medicos) {
            List<HorarioMedico> horarios = horarioMedicoRepository.findByMedicoIdOrderByDiaSemanaAsc(m.getId());
            Map<String, HorarioMedico> mapaDias = new HashMap<>();
            for (HorarioMedico h : horarios) {
                mapaDias.put(h.getDiaSemana().name(), h);
            }
            horariosPorMedico.put(m.getId(), mapaDias);
        }
        model.addAttribute("medicos", medicos);
        model.addAttribute("horariosPorMedico", horariosPorMedico);
        model.addAttribute("dias", DiaSemana.values());

        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "disponibilidad/listar";
    }

    @GetMapping("/historias")
    public String historias() { return "historias/listar"; }

    @GetMapping("/reportes")
    public String reportes() { return "reportes/listar"; }

    @GetMapping("/historias/gestion")
    public String historiasGestion(Model model) {
        model.addAttribute("pacientes", pacienteRepository.findAll());
        return "historias/gestion";
    }

    @GetMapping("/historias/consulta")
    public String historiasConsulta(Model model, Authentication auth) {
        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
        Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());
        boolean esPaciente = roles.contains("ROLE_PACIENTE");

        if (esPaciente && usuario != null) {
            Paciente paciente = pacienteRepository.findByEmail(usuario.getEmail()).orElse(null);
            List<Paciente> misPacientes = new ArrayList<>();
            if (paciente != null) misPacientes.add(paciente);
            model.addAttribute("pacientes", misPacientes);
            model.addAttribute("esPaciente", true);
        } else {
            model.addAttribute("pacientes", pacienteRepository.findAll());
            model.addAttribute("esPaciente", false);
        }
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "historias/consulta";
    }

    @GetMapping("/reportes/generar")
    public String reportesGenerar(Model model, Authentication auth) {
        model.addAttribute("totalPacientes", pacienteRepository.count());
        model.addAttribute("totalCitas", citaRepository.count());
        model.addAttribute("totalMedicos", medicoRepository.count());
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        
        // ⭐ LISTAS AGREGADAS PARA LAS TABLAS Y EXPORTACIONES ⭐
        model.addAttribute("medicos", medicoRepository.findAll());
        model.addAttribute("citas", citaRepository.findAll());
        
        Usuario usuario = usuarioRepository.findByUsername(auth.getName()).orElse(null);
        if(usuario != null) model.addAttribute("notificacionesNoLeidasGlobal", notificacionService.contarNoLeidas(usuario.getId()));
        return "reportes/generar";
    }
}