package com.restoria.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
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

    static {
        // POST /api/chat/stream processa a resposta em background (virtual
        // thread) e o Tomcat re-despacha a requisicao de forma assincrona
        // quando o SseEmitter e concluido. O SecurityContext por padrao
        // (MODE_THREADLOCAL) nao atravessa esses limites de thread, o que
        // faz o AuthorizationFilter negar acesso nesse dispatch assincrono
        // mesmo com um token valido. MODE_INHERITABLETHREADLOCAL propaga o
        // contexto para threads filhas (inclusive as criadas por
        // Executors.newVirtualThreadPerTaskExecutor()), resolvendo isso.
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
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
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
