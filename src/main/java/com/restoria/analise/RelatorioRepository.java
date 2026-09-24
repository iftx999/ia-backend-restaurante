package com.restoria.analise;

import com.restoria.shared.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RelatorioRepository extends JpaRepository<Relatorio, Long> {

    /**
     * Fetch join de {@code indicadoresPrato}: o relatorio e usado fora de
     * transacao (export de PDF/Excel, resposta do controller), entao a
     * colecao lazy precisa vir carregada de uma vez — sem isso, acessa-la
     * depois falha com "no Session" (mesma categoria de problema resolvida
     * em {@link Relatorio#getConteudoTextoIA()}, mas para colecoes).
     */
    @Query("select r from Relatorio r left join fetch r.indicadoresPrato where r.id = :id and r.usuario = :usuario")
    Optional<Relatorio> findByIdAndUsuario(@Param("id") Long id, @Param("usuario") Usuario usuario);

    /**
     * RF-15: historico de relatorios do usuario, mais recente primeiro. Sem
     * fetch join de {@code indicadoresPrato} — a listagem (resumo) nao usa
     * o detalhe por prato, entao evita carregar colecoes desnecessarias.
     */
    List<Relatorio> findByUsuarioOrderByGeradoEmDesc(Usuario usuario);

    /** RF-16: relatorio mais recente do usuario, usado pro banner de alerta proativo no chat. */
    Optional<Relatorio> findFirstByUsuarioOrderByGeradoEmDesc(Usuario usuario);
}
