package com.restoria.imagem;

import com.restoria.shared.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ImagemPratoRepository extends JpaRepository<ImagemPrato, Long> {

    /** Usado por {@code LimiteUsoService} para contar imagens geradas/editadas pelo usuario no periodo (mes corrente). */
    long countByUsuarioAndCriadaEmBetween(Usuario usuario, LocalDateTime inicio, LocalDateTime fim);

    Optional<ImagemPrato> findByIdAndUsuario(Long id, Usuario usuario);
}
