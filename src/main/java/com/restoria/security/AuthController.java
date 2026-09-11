package com.restoria.security;

import com.restoria.security.dto.AuthResponse;
import com.restoria.security.dto.LoginRequest;
import com.restoria.security.dto.RegistroRequest;
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
 * </ul>
 * Ambas as rotas sao publicas (ver SecurityConfig). Todas as demais rotas exigem
 * {@code Authorization: Bearer <token>}.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
