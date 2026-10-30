package com.bemfeito.api.common.error;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG=LoggerFactory.getLogger(ApiExceptionHandler.class);

    private ResponseEntity<ApiError> error(int status,String message,Map<String,String> fields) {
        return ResponseEntity.status(status).contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .body(new ApiError(Instant.now(),status,message,fields));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException exception) {
        return error(exception.getStatusCode().value(),exception.getReason(),Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        Map<String,String> fields=new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(e->fields.putIfAbsent(e.getField(),e.getDefaultMessage()));
        return error(400,"Dados inválidos",fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> validation(ConstraintViolationException exception) {
        Map<String,String> fields=new TreeMap<>();
        exception.getConstraintViolations().forEach(e->fields.putIfAbsent(e.getPropertyPath().toString(),e.getMessage()));
        return error(400,"Dados inválidos",fields);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> malformed(Exception exception) {
        return error(400,"JSON ou parâmetro inválido; confira nomes, tipos, enums e campos permitidos",Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        return error(409,"Conflito de dados: registro duplicado ou relacionamento inválido",Map.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException exception) {
        return error(405,"Método não permitido",Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> missing(NoResourceFoundException exception) {
        return error(404,"Recurso não encontrado",Map.of());
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<ApiError> denied(org.springframework.security.access.AccessDeniedException exception) {
        return error(403,"Acesso negado",Map.of());
    }
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    ResponseEntity<ApiError> unauthenticated(org.springframework.security.core.AuthenticationException exception) {
        return error(401,"Autenticação necessária",Map.of());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            return error(frameworkError.getStatusCode().value(),"Requisição não suportada",Map.of());
        }
        // Exception messages/causes may include SQL parameters or submitted secrets.
        LOG.error("Erro inesperado na API: tipo={}",exception.getClass().getSimpleName());
        return error(500,"Erro interno ao processar a solicitação",Map.of());
    }
}
