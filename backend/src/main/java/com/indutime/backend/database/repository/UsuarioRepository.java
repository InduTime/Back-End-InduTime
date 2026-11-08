package com.indutime.backend.database.repository;

import com.indutime.backend.database.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    List<Usuario> findAllByDeletedFalse();

    Optional<Usuario> findByIdUsuarioAndDeletedFalse(Long idUsuario);

    Optional<Usuario> findByEmailAndDeletedFalse(String email);

    Optional<Usuario> findByIdUsuarioAndDeletedTrue(Long idUsuario);

    Optional<Usuario> findByEmailAndDeletedTrue(String email);

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdUsuarioNot(String email, Long idUsuario);
}