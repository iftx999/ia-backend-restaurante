package com.restoria.shared;

import com.restoria.analise.PlanilhaInvalidaException;
import com.restoria.analise.RelatorioNaoEncontradoException;
import com.restoria.analise.UploadNaoEncontradoException;
import com.restoria.assinatura.AssinaturaIndisponivelException;
import com.restoria.assinatura.AssinaturaNaoEncontradaException;
import com.restoria.assinatura.LimiteUsoExcedidoException;
import com.restoria.assinatura.WebhookInvalidoException;
import com.restoria.chat.ConversaNaoEncontradaException;
import com.restoria.chat.ImagemInvalidaException;
import com.restoria.imagem.ImagemPratoNaoEncontradaException;
import com.restoria.integration.ai.AiConsultantException;
import com.restoria.integration.ai.ImagemIaException;
import com.restoria.security.CredenciaisInvalidasException;
import com.restoria.security.EmailJaCadastradoException;
import com.restoria.security.MuitasTentativasException;
import com.restoria.security.ReenvioVerificacaoMuitoRapidoException;
import com.restoria.security.TokenVerificacaoInvalidoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.de(HttpStatus.BAD_REQUEST.value(), "Requisicao invalida", detalhes));
    }

    @ExceptionHandler(ImagemInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handleImagemInvalida(ImagemInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.de(HttpStatus.BAD_REQUEST.value(), "Imagem invalida", List.of(ex.getMessage())));
    }

    @ExceptionHandler(ConversaNaoEncontradaException.class)
    public ResponseEntity<ApiErrorResponse> handleConversaNaoEncontrada(ConversaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(HttpStatus.NOT_FOUND.value(), "Conversa nao encontrada", List.of(ex.getMessage())));
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ApiErrorResponse> handleCredenciaisInvalidas(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.de(HttpStatus.UNAUTHORIZED.value(), "Credenciais invalidas", List.of(ex.getMessage())));
    }

    @ExceptionHandler(MuitasTentativasException.class)
    public ResponseEntity<ApiErrorResponse> handleMuitasTentativas(MuitasTentativasException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiErrorResponse.de(HttpStatus.TOO_MANY_REQUESTS.value(), "Muitas tentativas", List.of(ex.getMessage())));
    }

    @ExceptionHandler(ReenvioVerificacaoMuitoRapidoException.class)
    public ResponseEntity<ApiErrorResponse> handleReenvioMuitoRapido(ReenvioVerificacaoMuitoRapidoException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiErrorResponse.de(HttpStatus.TOO_MANY_REQUESTS.value(), "Aguarde antes de reenviar", List.of(ex.getMessage())));
    }

    @ExceptionHandler(TokenVerificacaoInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenVerificacaoInvalido(TokenVerificacaoInvalidoException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.de(HttpStatus.BAD_REQUEST.value(), "Token de verificacao invalido", List.of(ex.getMessage())));
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailJaCadastrado(EmailJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.de(HttpStatus.CONFLICT.value(), "Email ja cadastrado", List.of(ex.getMessage())));
    }

    @ExceptionHandler(PlanilhaInvalidaException.class)
    public ResponseEntity<ApiErrorResponse> handlePlanilhaInvalida(PlanilhaInvalidaException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.de(HttpStatus.BAD_REQUEST.value(), "Planilha invalida", ex.getErros()));
    }

    @ExceptionHandler(UploadNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleUploadNaoEncontrado(UploadNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(HttpStatus.NOT_FOUND.value(), "Upload nao encontrado", List.of(ex.getMessage())));
    }

    @ExceptionHandler(RelatorioNaoEncontradoException.class)
    public ResponseEntity<ApiErrorResponse> handleRelatorioNaoEncontrado(RelatorioNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(HttpStatus.NOT_FOUND.value(), "Relatorio nao encontrado", List.of(ex.getMessage())));
    }

    @ExceptionHandler(LimiteUsoExcedidoException.class)
    public ResponseEntity<ApiErrorResponse> handleLimiteUsoExcedido(LimiteUsoExcedidoException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(ApiErrorResponse.de(HttpStatus.PAYMENT_REQUIRED.value(), "Limite de uso do plano atingido", List.of(ex.getMessage())));
    }

    @ExceptionHandler(AssinaturaIndisponivelException.class)
    public ResponseEntity<ApiErrorResponse> handleAssinaturaIndisponivel(AssinaturaIndisponivelException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.de(HttpStatus.BAD_GATEWAY.value(), "Cobranca indisponivel", List.of(ex.getMessage())));
    }

    @ExceptionHandler(WebhookInvalidoException.class)
    public ResponseEntity<ApiErrorResponse> handleWebhookInvalido(WebhookInvalidoException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.de(HttpStatus.BAD_REQUEST.value(), "Webhook invalido", List.of(ex.getMessage())));
    }

    @ExceptionHandler(AssinaturaNaoEncontradaException.class)
    public ResponseEntity<ApiErrorResponse> handleAssinaturaNaoEncontrada(AssinaturaNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(HttpStatus.NOT_FOUND.value(), "Assinatura nao encontrada", List.of(ex.getMessage())));
    }

    @ExceptionHandler(AiConsultantException.class)
    public ResponseEntity<ApiErrorResponse> handleFalhaIa(AiConsultantException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.de(
                        HttpStatus.BAD_GATEWAY.value(),
                        "Nao foi possivel obter resposta da IA no momento",
                        List.of(ex.getMessage())));
    }

    /**
     * Prompt recusado por moderacao de conteudo (RF geracao de imagem) e um
     * erro do usuario (422), nao uma falha do provedor (502) — ver
     * {@link ImagemIaException#isRecusadaPorModeracao()}.
     */
    @ExceptionHandler(ImagemIaException.class)
    public ResponseEntity<ApiErrorResponse> handleFalhaImagemIa(ImagemIaException ex) {
        HttpStatus status = ex.isRecusadaPorModeracao() ? HttpStatus.UNPROCESSABLE_ENTITY : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.de(status.value(), "Nao foi possivel gerar a imagem", List.of(ex.getMessage())));
    }

    @ExceptionHandler(ImagemPratoNaoEncontradaException.class)
    public ResponseEntity<ApiErrorResponse> handleImagemPratoNaoEncontrada(ImagemPratoNaoEncontradaException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.de(HttpStatus.NOT_FOUND.value(), "Imagem nao encontrada", List.of(ex.getMessage())));
    }
}
