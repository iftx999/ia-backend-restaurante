package com.restoria.security;

import com.restoria.security.dto.AuthResponse;
import com.restoria.security.dto.LoginRequest;
import com.restoria.security.dto.RegistroRequest;
import com.restoria.security.dto.VerificarEmailRequest;
import com.restoria.shared.Usuario;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato:
 * <ul>
 *   <li>{@code POST /api/auth/registrar} — body: {@link RegistroRequest} (nome, email, senha,
 *       nomeRestaurante) — retorna 201 + {@link AuthResponse} (token, nome, email)</li>
 *   <li>{@code POST /api/auth/login} — body: {@link LoginRequest} (email, senha) — retorna 200 +
 *       {@link AuthResponse}</li>
 *   <li>{@code POST /api/auth/verificar-email} — body: {@link VerificarEmailRequest} (token, do link
 *       enviado no cadastro) — retorna 200 sem corpo. Publica (usuario pode nao estar logado no
 *       dispositivo que abriu o link).</li>
 *   <li>{@code POST /api/auth/reenviar-verificacao} — sem corpo, exige usuario autenticado — reenvia
 *       o e-mail de verificacao (novo token) pro usuario da requisicao.</li>
 * </ul>
 * {@code registrar}, {@code login} e {@code verificar-email} sao publicas (ver SecurityConfig);
 * {@code reenviar-verificacao} exige {@code Authorization: Bearer <token>} normal, porque precisa
 * saber pra qual usuario reenviar.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UsuarioAutenticadoProvider usuarioAutenticadoProvider;

    public AuthController(AuthService authService, UsuarioAutenticadoProvider usuarioAutenticadoProvider) {
        this.authService = authService;
        this.usuarioAutenticadoProvider = usuarioAutenticadoProvider;
    }

    @PostMapping("/registrar")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<Void> verificarEmail(@Valid @RequestBody VerificarEmailRequest request) {
        authService.verificarEmail(request.token());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<Void> reenviarVerificacao() {
        Usuario usuario = usuarioAutenticadoProvider.obterAtual();
        authService.reenviarVerificacao(usuario);
        return ResponseEntity.ok().build();
    }
}
