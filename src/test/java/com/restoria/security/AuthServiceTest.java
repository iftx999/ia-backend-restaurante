package com.restoria.security;

import com.restoria.assinatura.Assinatura;
import com.restoria.assinatura.AssinaturaRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService, assinaturaRepository);
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
}
