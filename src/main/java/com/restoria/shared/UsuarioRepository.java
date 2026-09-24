package com.restoria.shared;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByTokenVerificacaoEmail(String token);

    /**
     * SELECT ... FOR UPDATE na linha do usuario: serializa as reservas de uso
     * simultaneas do mesmo usuario (ver {@code LimiteUsoService}).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> travarPorId(@Param("id") Long id);
}
