package com.restoria.analise;

import com.restoria.shared.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadPlanilhaRepository extends JpaRepository<UploadPlanilha, Long> {

    Optional<UploadPlanilha> findByIdAndUsuario(Long id, Usuario usuario);
}
