package com.restoria.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuracao do Spring Security (RF-05). Autenticacao via JWT stateless:
 * login/registro em /api/auth/** sao publicos, todo o resto exige um
 * "Authorization: Bearer <token>" valido (ver JwtAuthenticationFilter).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final int portaGerenciamento;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${management.server.port:-1}") int portaGerenciamento) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.portaGerenciamento = portaGerenciamento;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Quando um SseEmitter termina com erro (POST /api/chat/stream),
                        // o Tomcat despacha internamente para o tratamento de erro
                        // padrao do Spring nessa mesma requisicao, numa thread sem
                        // SecurityContext valido — sem isso o AuthorizationFilter
                        // barra esse dispatch interno mesmo apos a resposta real
                        // (o evento "error" do SSE) ja ter sido entregue ao cliente.
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        // Webhook do Stripe: sem JWT (vem do Stripe, nao do frontend). A
                        // autenticidade e validada pela assinatura do payload contra
                        // STRIPE_WEBHOOK_SECRET dentro do proprio AssinaturaController,
                        // nao pelo filtro de seguranca.
                        // So os pontos de entrada sem sessao ainda existente sao publicos.
                        // /api/auth/reenviar-verificacao fica de fora de proposito: precisa
                        // saber pra qual usuario reenviar, entao exige o Bearer token normal.
                        .requestMatchers(
                                "/api/auth/registrar",
                                "/api/auth/login",
                                "/api/auth/verificar-email",
                                "/api/health",
                                "/api/assinatura/webhook")
                        .permitAll()
                        // Actuator (health/prometheus) so na porta de gerenciamento
                        // (MANAGEMENT_PORT), que nao e publicada na internet — o
                        // Prometheus coleta pela rede privada, sem JWT.
                        .requestMatchers(request -> portaGerenciamento > 0
                                && request.getLocalPort() == portaGerenciamento)
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
