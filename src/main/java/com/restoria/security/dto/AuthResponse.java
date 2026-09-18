package com.restoria.security.dto;

public record AuthResponse(String token, String nome, String email, boolean onboardingConcluido, boolean emailVerificado) {
}
