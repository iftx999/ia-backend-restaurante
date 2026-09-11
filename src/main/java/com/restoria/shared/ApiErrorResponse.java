package com.restoria.shared;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(Instant timestamp, int status, String erro, List<String> detalhes) {

    public static ApiErrorResponse de(int status, String erro, List<String> detalhes) {
        return new ApiErrorResponse(Instant.now(), status, erro, detalhes);
    }
}
