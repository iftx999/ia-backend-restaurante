package com.restoria.chat;

import com.restoria.shared.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversa_chat")
@Getter
@Setter
@NoArgsConstructor
public class ConversaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private LocalDateTime iniciadaEm;

    /**
     * Primeira pergunta do usuario (truncada), gravada na criacao da conversa.
     * Evita uma consulta extra por conversa ao listar a sidebar (N+1).
     */
    @Column(length = TAMANHO_MAX_TITULO)
    private String titulo;

    public static final int TAMANHO_MAX_TITULO = 200;

    public ConversaChat(Usuario usuario, String primeiraMensagem) {
        this.usuario = usuario;
        this.iniciadaEm = LocalDateTime.now();
        this.titulo = primeiraMensagem == null || primeiraMensagem.length() <= TAMANHO_MAX_TITULO
                ? primeiraMensagem
                : primeiraMensagem.substring(0, TAMANHO_MAX_TITULO);
    }
}
