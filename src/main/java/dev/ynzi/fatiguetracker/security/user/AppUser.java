package dev.ynzi.fatiguetracker.security.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Compte applicatif (J3). Le mot de passe est toujours stocké sous forme de hash
 * BCrypt (voir {@link dev.ynzi.fatiguetracker.security.SecurityConfig#passwordEncoder()}),
 * jamais en clair. Deux comptes de démonstration sont seedés par la migration Flyway
 * {@code V4__app_user.sql} (identifiants documentés dans le README).
 */
@Entity
@Table(name = "app_user", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "username"))
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    /** Hash BCrypt, jamais le mot de passe en clair. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected AppUser() {
        // requis par JPA
    }

    public AppUser(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppUser appUser)) {
            return false;
        }
        if (id != null && appUser.id != null) {
            return id.equals(appUser.id);
        }
        return Objects.equals(username, appUser.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "AppUser{id=%s, username='%s', role=%s}".formatted(id, username, role);
    }
}
