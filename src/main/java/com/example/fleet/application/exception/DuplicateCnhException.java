package com.example.fleet.application.exception;

/**
 * Thrown by {@link com.example.fleet.application.service.DriverService} when a
 * {@code CreateDriverCommand} contains a CNH that is already registered in the
 * {@code DriverRepository}.
 * Maps to HTTP 409 in the global exception handler.
 *
 * <p><strong>LGPD / PII notice:</strong> The CNH (Carteira Nacional de Habilitação)
 * is a government-issued document number and qualifies as personal data under
 * Brazil's Lei Geral de Proteção de Dados (LGPD). Although the raw CNH value is
 * stored in this exception's message for internal stack-trace debugging,
 * {@code GlobalExceptionHandler} <em>must not</em> echo it in HTTP response bodies
 * or application logs. The handler must return a generic {@code "CNH already registered"}
 * message and log only a generic warning with no PII.</p>
 */
public class DuplicateCnhException extends RuntimeException {

    public DuplicateCnhException(String cnh) {
        super("CNH already registered: " + cnh);
    }
}
