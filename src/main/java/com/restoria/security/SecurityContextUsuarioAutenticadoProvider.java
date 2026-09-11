package com.restoria.security;

import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextUsuarioAutenticadoProvider implements UsuarioAutenticadoProvider {

    private final UsuarioRepository usuarioRepository;

    public SecurityContextUsuarioAutenticadoProvider(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Usuario obterAtual() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new UsuarioNaoAutenticadoException("Nenhum usuario autenticado na requisicao atual");
        }

        String email = autenticacao.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsuarioNaoAutenticadoException(
                        "Usuario autenticado nao encontrado: " + email));
    }
}
