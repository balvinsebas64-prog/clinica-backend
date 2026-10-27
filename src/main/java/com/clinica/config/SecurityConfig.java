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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
            // ⭐ RNF18: HEADERS DE SEGURIDAD
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(content -> {})
                .xssProtection(xss -> {})
                .cacheControl(cache -> {})
            )

            .authorizeHttpRequests(auth -> auth
                // ==========================================
                // RUTAS PÚBLICAS
                // ==========================================
                .requestMatchers("/login", "/registro", "/registro/guardar", "/publico",
                                 "/css/**", "/js/**", "/img/**", "/error/**").permitAll()

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
                // DISPONIBILIDAD
                // ==========================================
                .requestMatchers("/disponibilidad").hasAnyRole("ADMIN", "MEDICO", "PACIENTE", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // NOTIFICACIONES
                // ==========================================
                .requestMatchers("/notificaciones").hasAnyRole("ADMIN", "MEDICO", "PACIENTE", "PERSONAL_ADMINISTRATIVO")

                // ==========================================
                // REPORTES - Solo admin
                // ==========================================
                .requestMatchers("/reportes/**").hasRole("ADMIN")

                // ==========================================
                // PACIENTES
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
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // ⭐ RNF18: CSRF habilitado por defecto (Spring Security ya lo hace)
            .csrf(csrf -> csrf.ignoringRequestMatchers(
                new AntPathRequestMatcher("/h2-console/**")
            ));

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