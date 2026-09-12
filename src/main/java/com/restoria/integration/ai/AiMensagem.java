package com.restoria.integration.ai;

/**
 * Uma mensagem no formato aceito pela API da Anthropic (role "user"/"assistant").
 *
 * @param imagemBase64     dados da imagem anexada (base64, sem o prefixo "data:"), ou null
 *                         se a mensagem nao tem imagem. So relevante para role "user" — nao
 *                         e persistida no historico da conversa, so usada no turno atual.
 * @param imagemMediaType  media type da imagem (ex: "image/png"), ou null se sem imagem.
 */
public record AiMensagem(String role, String conteudo, String imagemBase64, String imagemMediaType) {

    public static AiMensagem doUsuario(String conteudo) {
        return new AiMensagem("user", conteudo, null, null);
    }

    public static AiMensagem doUsuarioComImagem(String conteudo, String imagemBase64, String imagemMediaType) {
        return new AiMensagem("user", conteudo, imagemBase64, imagemMediaType);
    }

    public static AiMensagem daIa(String conteudo) {
        return new AiMensagem("assistant", conteudo, null, null);
    }

    public boolean temImagem() {
        return imagemBase64 != null && !imagemBase64.isBlank();
    }
}
