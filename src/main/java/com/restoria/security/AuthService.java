package com.restoria.security;

import com.restoria.assinatura.Assinatura;
import com.restoria.assinatura.AssinaturaRepository;
import com.restoria.security.dto.AuthResponse;
import com.restoria.security.dto.LoginRequest;
import com.restoria.security.dto.RegistroRequest;
import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro/login de usuarios (RF-05).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AssinaturaRepository assinaturaRepository;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AssinaturaRepository assinaturaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        usuarioRepository.findByEmail(request.email()).ifPresent(u -> {
            throw new EmailJaCadastradoException(request.email());
        });

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha()),
                request.nomeRestaurante());
        usuario = usuarioRepository.save(usuario);

        // Todo usuario nasce com uma assinatura no plano gratis — nunca fica sem.
        assinaturaRepository.save(new Assinatura(usuario));

        String token = jwtService.gerarToken(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.isOnboardingConcluido());
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new CredenciaisInvalidasException();
        }

        String token = jwtService.gerarToken(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.isOnboardingConcluido());
    }
}
