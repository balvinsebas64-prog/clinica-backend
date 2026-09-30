package com.clinica.config;

import com.clinica.servicios.UsuarioService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.Set;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioService usuarioService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // ==========================================
                // RUTAS PÚBLICAS
                // ==========================================
                .requestMatchers("/login", "/registro", "/registro/guardar",
                                 "/css/**", "/js/**", "/img/**").permitAll()

                // ==========================================
                // RUTAS POR ROL
                // ==========================================
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/doctor/**").hasRole("MEDICO")
                .requestMatchers("/paciente/**").hasRole("PACIENTE")
                .requestMatchers("/personal/**").hasRole("PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // CITAS - Acceso para varios roles
                // ==========================================
                .requestMatchers("/citas/programar").hasAnyRole("ADMIN", "PACIENTE", "PERSONAL_ADMINISTRATIVO")
                .requestMatchers("/citas/reprogramar").hasAnyRole("ADMIN", "MEDICO", "PERSONAL_ADMINISTRATIVO")
                .requestMatchers("/citas/cancelar").hasAnyRole("ADMIN", "PERSONAL_ADMINISTRATIVO")
                .requestMatchers("/citas/**").hasAnyRole("ADMIN", "MEDICO", "PACIENTE", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // HISTORIAS - Acceso para varios roles
                // ==========================================
                .requestMatchers("/historias/gestion").hasAnyRole("ADMIN", "MEDICO")
                .requestMatchers("/historias/consulta").hasAnyRole("ADMIN", "MEDICO", "PACIENTE")
                .requestMatchers("/historias/**").hasAnyRole("ADMIN", "MEDICO")

                // ==========================================
                // DISPONIBILIDAD - Acceso para varios roles
                // ==========================================
                .requestMatchers("/disponibilidad").hasAnyRole("ADMIN", "MEDICO", "PACIENTE", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // NOTIFICACIONES - Acceso para todos
                // ==========================================
                .requestMatchers("/notificaciones").hasAnyRole("ADMIN", "MEDICO", "PACIENTE", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // REPORTES - Solo admin
                // ==========================================
                .requestMatchers("/reportes/**").hasRole("ADMIN")

                // ==========================================
                // PACIENTES - Admin, Médico, Personal
                // ==========================================
                .requestMatchers("/pacientes/**").hasAnyRole("ADMIN", "MEDICO", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // CUALQUIER OTRA RUTA requiere autenticación
                // ==========================================
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                Authentication authentication) throws IOException, ServletException {
                Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

                if (roles.contains("ROLE_ADMIN")) {
                    response.sendRedirect("/admin");
                } else if (roles.contains("ROLE_MEDICO")) {
                    response.sendRedirect("/doctor");
                } else if (roles.contains("ROLE_PACIENTE")) {
                    response.sendRedirect("/paciente");
                } else if (roles.contains("ROLE_PERSONAL_ADMINISTRATIVO")) {
                    response.sendRedirect("/personal");
                } else {
                    response.sendRedirect("/");
                }
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(usuarioService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}