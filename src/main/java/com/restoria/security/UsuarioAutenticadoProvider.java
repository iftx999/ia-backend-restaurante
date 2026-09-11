package com.restoria.security;

import com.restoria.shared.Usuario;

/**
 * Abstrai o acesso ao usuario autenticado da requisicao atual. Existe para
 * que Services (ex: ChatService) nao precisem depender diretamente de
 * SecurityContextHolder (estatico, dificil de mockar em testes).
 */
public interface UsuarioAutenticadoProvider {

    Usuario obterAtual();
}
