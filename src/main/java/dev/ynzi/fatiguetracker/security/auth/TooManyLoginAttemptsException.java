package dev.ynzi.fatiguetracker.security.auth;

/** Levée lorsqu'un utilisateur dépasse le nombre d'échecs de connexion autorisé. */
public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException(String username) {
        super("Trop de tentatives de connexion pour l'utilisateur : " + username);
    }
}
