package com.restoria.security;

import com.restoria.assinatura.Assinatura;
import com.restoria.assinatura.AssinaturaRepository;
import com.restoria.integration.email.EmailProperties;
import com.restoria.integration.email.EmailSender;
import com.restoria.security.dto.AuthResponse;
import com.restoria.security.dto.LoginRequest;
import com.restoria.security.dto.RegistroRequest;
import com.restoria.shared.Usuario;
import com.restoria.shared.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Registro/login de usuarios (RF-05) + verificacao de e-mail (ideia
 * levantada em 2026-09-13, ver docs/04-roadmap.md).
 */
@Service
public class AuthService {

    /** Tempo de validade do link de verificacao antes do usuario precisar pedir um novo. */
    private static final long TOKEN_VERIFICACAO_VALIDADE_HORAS = 24;

    /** Intervalo minimo entre pedidos de reenvio (mitigacao de spam/flood). */
    private static final long REENVIO_COOLDOWN_SEGUNDOS = 60;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AssinaturaRepository assinaturaRepository;
    private final EmailSender emailSender;
    private final EmailProperties emailProperties;
    private final LoginRateLimiter loginRateLimiter;

    public AuthService(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AssinaturaRepository assinaturaRepository,
            EmailSender emailSender,
            EmailProperties emailProperties,
            LoginRateLimiter loginRateLimiter) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
        this.assinaturaRepository = assinaturaRepository;
        this.emailSender = emailSender;
        this.emailProperties = emailProperties;
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
        gerarNovoTokenVerificacao(usuario);
        usuario = usuarioRepository.save(usuario);

        // Todo usuario nasce com uma assinatura no plano gratis — nunca fica sem.
        assinaturaRepository.save(new Assinatura(usuario));

        enviarEmailVerificacao(usuario);

        String token = jwtService.gerarToken(usuario);
        return new AuthResponse(
                token, usuario.getNome(), usuario.getEmail(), usuario.isOnboardingConcluido(), usuario.isEmailVerificado());
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiter.verificarLiberado(request.email());

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> falhaLogin(request.email()));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw falhaLogin(request.email());
        }

        loginRateLimiter.registrarSucesso(request.email());
        String token = jwtService.gerarToken(usuario);
        return new AuthResponse(
                token, usuario.getNome(), usuario.getEmail(), usuario.isOnboardingConcluido(), usuario.isEmailVerificado());
    }

    private CredenciaisInvalidasException falhaLogin(String email) {
        loginRateLimiter.registrarFalha(email);
        return new CredenciaisInvalidasException();
    }

    /**
     * Confirma o e-mail a partir do token do link enviado no cadastro. Nao
     * bloqueia login/uso do chat antes disso (decisao de produto) — so marca
     * a conta como verificada.
     */
    @Transactional
    public void verificarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacaoEmail(token)
                .orElseThrow(TokenVerificacaoInvalidoException::new);

        if (usuario.getTokenVerificacaoExpiraEm() == null
                || usuario.getTokenVerificacaoExpiraEm().isBefore(LocalDateTime.now())) {
            throw new TokenVerificacaoInvalidoException();
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacaoEmail(null);
        usuario.setTokenVerificacaoExpiraEm(null);
        usuarioRepository.save(usuario);
    }

    /**
     * Gera um novo link e reenvia — usado quando o usuario pede reenvio (token
     * anterior expirado/perdido). Cooldown entre pedidos (levantado em revisao
     * de seguranca, 2026-09-13) evita que o endpoint autenticado seja usado
     * pra floodar a caixa de entrada do proprio usuario.
     *
     * @throws ReenvioVerificacaoMuitoRapidoException se o ultimo envio foi ha menos de {@link #REENVIO_COOLDOWN_SEGUNDOS}
     */
    @Transactional
    public void reenviarVerificacao(Usuario usuario) {
        if (usuario.isEmailVerificado()) {
            return;
        }

        LocalDateTime ultimoEnvio = usuario.getTokenVerificacaoEnviadoEm();
        if (ultimoEnvio != null && ultimoEnvio.isAfter(LocalDateTime.now().minusSeconds(REENVIO_COOLDOWN_SEGUNDOS))) {
            throw new ReenvioVerificacaoMuitoRapidoException();
        }

        gerarNovoTokenVerificacao(usuario);
        usuarioRepository.save(usuario);
        enviarEmailVerificacao(usuario);
    }

    private void gerarNovoTokenVerificacao(Usuario usuario) {
        usuario.setTokenVerificacaoEmail(UUID.randomUUID().toString());
        usuario.setTokenVerificacaoExpiraEm(LocalDateTime.now().plusHours(TOKEN_VERIFICACAO_VALIDADE_HORAS));
        usuario.setTokenVerificacaoEnviadoEm(LocalDateTime.now());
    }

    private void enviarEmailVerificacao(Usuario usuario) {
        String link = emailProperties.linkBase() + "/verificar-email?token=" + usuario.getTokenVerificacaoEmail();
        emailSender.enviarVerificacaoEmail(usuario.getEmail(), usuario.getNome(), link);
    }
}
