package com.restoria.security;

import com.restoria.shared.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtProperties jwtProperties = new JwtProperties(
            "chave-de-teste-fixa-com-32-caracteres-ou-mais-1234", 480);
    private final JwtService jwtService = new JwtService(jwtProperties);

    @Test
    void geraTokenEExtraiEmailCorretamente() {
        Usuario usuario = new Usuario("Gerente Teste", "gerente@teste.com", "hash", "Restaurante Teste");

        String token = jwtService.gerarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extrairEmail(token)).isEqualTo("gerente@teste.com");
    }

    @Test
    void tokenAdulteradoOuInvalidoLancaTokenInvalidoException() {
        assertThatThrownBy(() -> jwtService.extrairEmail("token.invalido.aleatorio"))
                .isInstanceOf(TokenInvalidoException.class);
    }
}
