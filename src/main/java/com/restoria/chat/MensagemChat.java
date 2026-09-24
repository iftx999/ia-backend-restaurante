package com.restoria.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "mensagem_chat")
@Getter
@Setter
@NoArgsConstructor
public class MensagemChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    private ConversaChat conversa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AutorMensagem autor;

    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime enviadaEm;

    public MensagemChat(ConversaChat conversa, AutorMensagem autor, String conteudo) {
        this.conversa = conversa;
        this.autor = autor;
        this.conteudo = conteudo;
        this.enviadaEm = LocalDateTime.now();
    }
}
