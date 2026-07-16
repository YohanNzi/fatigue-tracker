package dev.ynzi.fatiguetracker.common;

import dev.ynzi.fatiguetracker.aircraft.AircraftNotFoundException;
import dev.ynzi.fatiguetracker.fatigue.FatigueRecomputeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Traduit les exceptions métier / de validation en réponses HTTP structurées. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AircraftNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(AircraftNotFoundException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(FatigueRecomputeException.class)
    public ResponseEntity<ApiError> handleFatigueRecompute(FatigueRecomputeException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    /** Identifiants invalides sur {@code POST /api/auth/login} (mauvais mot de passe, utilisateur inconnu). */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        ApiError body = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Identifiants invalides",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorDetail)
                .toList();

        ApiError body = ApiError.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation échouée pour un ou plusieurs champs",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ApiError.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return new ApiError.FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
