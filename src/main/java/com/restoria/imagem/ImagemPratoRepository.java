package com.restoria.imagem;

import com.restoria.shared.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImagemPratoRepository extends JpaRepository<ImagemPrato, Long> {

    Optional<ImagemPrato> findByIdAndUsuario(Long id, Usuario usuario);
}
