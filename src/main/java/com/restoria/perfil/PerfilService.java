package com.restoria.perfil;

import com.restoria.perfil.dto.AtualizarLimitesAnaliseRequest;
import com.restoria.perfil.dto.AtualizarPerfilRequest;
import com.restoria.perfil.dto.PerfilResponse;
import com.restoria.security.UsuarioAutenticadoProvider;
import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Perfil do usuario: onboarding conversacional (primeiro acesso ao chat, ver
 * ChatComponent no frontend) e limites de alerta do modulo analitico (RF-10).
 */
@Service
public class PerfilService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public PerfilService(UsuarioRepository usuarioRepository, UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    public PerfilResponse buscarPerfilAtual() {
        return PerfilResponse.de(usuarioAutenticadoProvider.obterAtual());
    }

    /**
     * A IA pergunta nome e nome do restaurante antes de liberar as demais
     * perguntas do chat. Os valores aqui sobrescrevem os informados no
     * cadastro, ja que a conversa e a fonte de verdade mais recente.
     */
    @Transactional
    public PerfilResponse concluirOnboarding(AtualizarPerfilRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        usuario.setNome(request.nome());
        usuario.setNomeRestaurante(request.nomeRestaurante());
        usuario.setOnboardingConcluido(true);
        usuario = usuarioRepository.save(usuario);
        return PerfilResponse.de(usuario);
    }

    @Transactional
    public PerfilResponse atualizarLimitesAnalise(AtualizarLimitesAnaliseRequest request) {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        usuario.setMargemMinimaEsperada(request.margemMinimaEsperada());
        usuario.setPercentualPerdaAlerta(request.percentualPerdaAlerta());
        usuario = usuarioRepository.save(usuario);
        return PerfilResponse.de(usuario);
    }
}
