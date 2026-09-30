package com.clinica.controladores;

import com.clinica.entidades.Medico;
import com.clinica.entidades.Rol;
import com.clinica.entidades.Usuario;
import com.clinica.repositorios.MedicoRepository;
import com.clinica.repositorios.RolRepository;
import com.clinica.repositorios.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private MedicoRepository medicoRepo;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioRepo.findAll());
        return "usuario/listar";
    }

    @GetMapping("/crear")
    public String crear(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepo.findAll());
        // ⭐ SOLO médicos SIN usuario
        model.addAttribute("medicos", medicoRepo.findByUsuarioIsNull());
        model.addAttribute("modoEdicion", false);
        return "usuario/crear";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        Usuario usuario = usuarioRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + id));
        model.addAttribute("usuario", usuario);
        model.addAttribute("roles", rolRepo.findAll());
        // ⭐ SOLO médicos SIN usuario
        model.addAttribute("medicos", medicoRepo.findByUsuarioIsNull());
        model.addAttribute("modoEdicion", true);
        return "usuario/crear";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Usuario usuario,
                          @RequestParam(value = "rolId", required = false) Integer rolId,
                          @RequestParam(value = "medicoId", required = false) Integer medicoId) {
        boolean esNuevo = (usuario.getId() == null);

        if (rolId != null) {
            Rol rol = rolRepo.findById(rolId).orElse(null);
            if (rol != null) {
                Set<Rol> roles = new HashSet<>();
                roles.add(rol);
                usuario.setRoles(roles);
            }
        }

        usuarioRepo.save(usuario);

        if (medicoId != null) {
            Medico medico = medicoRepo.findById(medicoId).orElse(null);
            if (medico != null && medico.getUsuario() == null) {
                medico.setUsuario(usuario);
                medicoRepo.save(medico);
            }
        }

        String mensaje = esNuevo ? "Usuario creado correctamente" : "Usuario actualizado correctamente";
        return "redirect:/usuarios?exito=" + mensaje;
    }

    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id) {
        usuarioRepo.deleteById(id);
        return "redirect:/usuarios?exito=Usuario eliminado correctamente";
    }
}