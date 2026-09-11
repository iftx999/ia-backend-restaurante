package com.restoria.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByConversaIdOrderByEnviadaEmAsc(Long conversaId);

    Optional<MensagemChat> findFirstByConversaIdAndAutorOrderByEnviadaEmAsc(Long conversaId, AutorMensagem autor);

    /** Usado por {@code LimiteUsoService} para contar mensagens do usuario no periodo (mes corrente). */
    long countByConversa_Usuario_IdAndAutorAndEnviadaEmBetween(
            Long usuarioId, AutorMensagem autor, LocalDateTime inicio, LocalDateTime fim);
}
