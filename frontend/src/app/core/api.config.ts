/**
 * Base de l'API back (Spring Boot). En dev, le front tourne sur :4200 et le back sur
 * :8080 (d'où le CORS activé côté SecurityConfig). Quand le front sera servi par Spring
 * (J5.4, même origin), passer à '' (chemins relatifs) — ou piloter via un environment.
 */
export const API_BASE = 'http://localhost:8080';
