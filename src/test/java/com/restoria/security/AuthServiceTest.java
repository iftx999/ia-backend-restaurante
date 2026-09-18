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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private EmailSender emailSender;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmailProperties emailProperties = new EmailProperties(true, "no-reply@teste.com", "http://localhost:4200");
    private final LoginRateLimiter loginRateLimiter = new LoginRateLimiter();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                usuarioRepository, passwordEncoder, jwtService, assinaturaRepository, emailSender, emailProperties,
                loginRateLimiter);
    }

    @Test
    void registrarCriaUsuarioComSenhaCodificadaEDevolveToken() {
        RegistroRequest request = new RegistroRequest("Gerente", "gerente@teste.com", "senha123", "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com")).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.gerarToken(any(Usuario.class))).thenReturn("token-gerado");

        AuthResponse resposta = authService.registrar(request);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario usuarioSalvo = usuarioCaptor.getValue();

        assertThat(usuarioSalvo.getSenha()).isNotEqualTo("senha123");
        assertThat(passwordEncoder.matches("senha123", usuarioSalvo.getSenha())).isTrue();
        assertThat(resposta.token()).isEqualTo("token-gerado");
        assertThat(resposta.email()).isEqualTo("gerente@teste.com");
        assertThat(resposta.nome()).isEqualTo("Gerente");
        assertThat(resposta.emailVerificado()).isFalse();
        assertThat(usuarioSalvo.getTokenVerificacaoEmail()).isNotBlank();
        assertThat(usuarioSalvo.getTokenVerificacaoExpiraEm()).isAfter(LocalDateTime.now());
        verify(emailSender).enviarVerificacaoEmail(
                eq("gerente@teste.com"), eq("Gerente"), contains(usuarioSalvo.getTokenVerificacaoEmail()));
    }

    @Test
    void registrarComEmailJaCadastradoLancaExcecao() {
        RegistroRequest request = new RegistroRequest("Gerente", "gerente@teste.com", "senha123", "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com"))
                .thenReturn(Optional.of(new Usuario("Outro", "gerente@teste.com", "hash", "Outro Restaurante")));

        assertThatThrownBy(() -> authService.registrar(request))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void loginComSenhaCorretaFunciona() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", passwordEncoder.encode("senha123"), "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(usuario)).thenReturn("token-gerado");

        AuthResponse resposta = authService.login(new LoginRequest("gerente@teste.com", "senha123"));

        assertThat(resposta.token()).isEqualTo("token-gerado");
        assertThat(resposta.email()).isEqualTo("gerente@teste.com");
    }

    @Test
    void loginComSenhaErradaLancaCredenciaisInvalidas() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", passwordEncoder.encode("senha123"), "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.login(new LoginRequest("gerente@teste.com", "senhaErrada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void loginComEmailInexistenteLancaCredenciaisInvalidas() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("naoexiste@teste.com", "qualquer")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    void verificarEmailComTokenValidoMarcaUsuarioComoVerificado() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setTokenVerificacaoEmail("token-valido");
        usuario.setTokenVerificacaoExpiraEm(LocalDateTime.now().plusHours(1));
        when(usuarioRepository.findByTokenVerificacaoEmail("token-valido")).thenReturn(Optional.of(usuario));

        authService.verificarEmail("token-valido");

        assertThat(usuario.isEmailVerificado()).isTrue();
        assertThat(usuario.getTokenVerificacaoEmail()).isNull();
        assertThat(usuario.getTokenVerificacaoExpiraEm()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void verificarEmailComTokenExpiradoLancaExcecao() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setTokenVerificacaoEmail("token-expirado");
        usuario.setTokenVerificacaoExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(usuarioRepository.findByTokenVerificacaoEmail("token-expirado")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> authService.verificarEmail("token-expirado"))
                .isInstanceOf(TokenVerificacaoInvalidoException.class);
        assertThat(usuario.isEmailVerificado()).isFalse();
    }

    @Test
    void verificarEmailComTokenInexistenteLancaExcecao() {
        when(usuarioRepository.findByTokenVerificacaoEmail("token-invalido")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verificarEmail("token-invalido"))
                .isInstanceOf(TokenVerificacaoInvalidoException.class);
    }

    @Test
    void reenviarVerificacaoGeraNovoTokenEEnviaDeNovoQuandoAindaNaoVerificado() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setTokenVerificacaoEmail("token-antigo");

        authService.reenviarVerificacao(usuario);

        assertThat(usuario.getTokenVerificacaoEmail()).isNotEqualTo("token-antigo");
        verify(usuarioRepository).save(usuario);
        verify(emailSender).enviarVerificacaoEmail(eq("gerente@teste.com"), eq("Gerente"), anyString());
    }

    @Test
    void reenviarVerificacaoNaoFazNadaQuandoJaVerificado() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setEmailVerificado(true);

        authService.reenviarVerificacao(usuario);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(emailSender, never()).enviarVerificacaoEmail(anyString(), anyString(), anyString());
    }

    @Test
    void reenviarVerificacaoLogoAposOutroPedidoLancaExcecaoDeCooldown() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setTokenVerificacaoEnviadoEm(LocalDateTime.now().minusSeconds(5));

        assertThatThrownBy(() -> authService.reenviarVerificacao(usuario))
                .isInstanceOf(ReenvioVerificacaoMuitoRapidoException.class);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void reenviarVerificacaoAposCooldownExpirarFunciona() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", "hash", "Restaurante Teste");
        usuario.setTokenVerificacaoEnviadoEm(LocalDateTime.now().minusMinutes(2));

        authService.reenviarVerificacao(usuario);

        verify(usuarioRepository).save(usuario);
        verify(emailSender).enviarVerificacaoEmail(eq("gerente@teste.com"), eq("Gerente"), anyString());
    }

    @Test
    void loginBloqueiaAposVariasFalhasSeguidas() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", passwordEncoder.encode("senhaCorreta"), "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("gerente@teste.com", "senhaErrada")))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        // Na 6a tentativa, mesmo com a senha certa, o rate limiter ja deve bloquear.
        assertThatThrownBy(() -> authService.login(new LoginRequest("gerente@teste.com", "senhaCorreta")))
                .isInstanceOf(MuitasTentativasException.class);
    }

    @Test
    void loginComSucessoLimpaContadorDeFalhasAnteriores() {
        Usuario usuario = new Usuario("Gerente", "gerente@teste.com", passwordEncoder.encode("senhaCorreta"), "Restaurante Teste");
        when(usuarioRepository.findByEmail("gerente@teste.com")).thenReturn(Optional.of(usuario));
        when(jwtService.gerarToken(usuario)).thenReturn("token-gerado");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("gerente@teste.com", "senhaErrada")))
                    .isInstanceOf(CredenciaisInvalidasException.class);
        }

        AuthResponse resposta = authService.login(new LoginRequest("gerente@teste.com", "senhaCorreta"));

        assertThat(resposta.token()).isEqualTo("token-gerado");
        loginRateLimiter.verificarLiberado("gerente@teste.com");
    }
}
