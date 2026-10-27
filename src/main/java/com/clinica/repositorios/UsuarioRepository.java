package com.clinica.repositorios;

import com.clinica.entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);

    // Búsqueda por texto (nombre, username o email)
    List<Usuario> findByUsernameContainingIgnoreCaseOrNombreCompletoContainingIgnoreCaseOrEmailContainingIgnoreCase(
        String username, String nombre, String email
    );

    // ⭐ NUEVO: Filtrar por el ID del Rol
    List<Usuario> findByRoles_Id(Integer rolId);
}