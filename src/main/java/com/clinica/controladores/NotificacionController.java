package com.clinica.controladores;

import com.clinica.entidades.Usuario;
import com.clinica.repositorios.UsuarioRepository;
import com.clinica.servicios.NotificacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody; // ⭐ IMPORTANTE

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    @Autowired
    private NotificacionService service;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @GetMapping
    public String listar(Model model, Authentication auth) {
        Usuario usuario = usuarioRepo.findByUsername(auth.getName()).orElse(null);
        if (usuario == null) return "redirect:/login";

        model.addAttribute("notificaciones", service.listar(usuario.getId()));
        model.addAttribute("noLeidas", service.contarNoLeidas(usuario.getId()));
        return "notificaciones/listar";
    }

    @GetMapping("/leer/{id}")
    public String leer(@PathVariable Integer id) {
        service.marcarLeida(id);
        return "redirect:/notificaciones";
    }

    @GetMapping("/leer-todas")
    public String leerTodas(Authentication auth) {
        Usuario usuario = usuarioRepo.findByUsername(auth.getName()).orElse(null);
        if (usuario != null) {
            service.marcarTodasLeidas(usuario.getId());
        }
        return "redirect:/notificaciones";
    }

    // ⭐ MÉTODO PARA EL CONTADOR DE NOTIFICACIONES (CAMPANITA)
    @GetMapping("/api/contador")
    @ResponseBody
    public java.util.Map<String, Long> contador(Authentication auth) {
        Usuario usuario = usuarioRepo.findByUsername(auth.getName()).orElse(null);
        java.util.Map<String, Long> response = new java.util.HashMap<>();
        
        if (usuario != null) {
            response.put("cantidad", service.contarNoLeidas(usuario.getId()));
        } else {
            response.put("cantidad", 0L);
        }
        return response;
    }
}