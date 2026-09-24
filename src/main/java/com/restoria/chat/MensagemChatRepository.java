package com.restoria.chat;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByConversaIdOrderByEnviadaEmAsc(Long conversaId);

    /** Ultimas mensagens da conversa (mais recente primeiro) — janela de historico enviada a IA. */
    List<MensagemChat> findByConversaIdOrderByEnviadaEmDesc(Long conversaId, Pageable pageable);

}
