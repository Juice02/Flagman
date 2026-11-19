package com.devjuice.flagman.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.stream.Collectors;

// Turns errors into JSON bodies (RFC 9457 "problem detail") with a readable "detail" message.
// The parent class already does this for ResponseStatusException and Spring's own errors
// (bad path value, wrong HTTP method, ...); the methods below cover the rest.
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    // Body is not valid JSON, or a field has the wrong type or value (e.g. environment "prod").
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        String detail = "Request body is not valid JSON";
        if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = mismatch.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                    .collect(Collectors.joining("."));
            detail = "Invalid value for field '" + field + "'";
        }
        return handleExceptionInternal(ex, ProblemDetail.forStatusAndDetail(status, detail), headers, status, request);
    }

    // A database constraint caught what the service checks missed, e.g. two requests creating the same flag at once.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

}
