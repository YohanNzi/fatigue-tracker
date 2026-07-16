package dev.ynzi.fatiguetracker.common;

import java.time.Instant;
import java.util.List;

/**
 * Corps de réponse structuré pour les erreurs de l'API.
 *
 * @param fieldErrors uniquement renseigné pour les erreurs de validation (400) ;
 *                     {@code null} sinon.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError ofValidation(int status, String error, String message, String path,
                                         List<FieldErrorDetail> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }

    public record FieldErrorDetail(String field, String message) {
    }
}
