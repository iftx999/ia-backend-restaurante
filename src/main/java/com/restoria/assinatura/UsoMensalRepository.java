package com.restoria.assinatura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsoMensalRepository extends JpaRepository<UsoMensal, Long> {

    Optional<UsoMensal> findByUsuarioIdAndCompetencia(Long usuarioId, String competencia);
}
