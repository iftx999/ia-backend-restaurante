package com.restoria.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversaChatRepository extends JpaRepository<ConversaChat, Long> {

    List<ConversaChat> findByUsuarioIdOrderByIniciadaEmDesc(Long usuarioId);
}
