/**
 * Base de l'API back (Spring Boot). Chaîne vide = chemins RELATIFS, donc même origine
 * que la page : en prod, le front est servi par Spring depuis le jar (mono-artefact),
 * l'API est sur la même origine → `/api/...` résout tout seul (HTTP→HTTPS respecté).
 *
 * En dev, le front tourne sur :4200 et le back sur :8080 : `ng serve` utilise
 * `proxy.config.json` pour relayer `/api` vers :8080 (câblé dans angular.json → serve →
 * development → proxyConfig). Ainsi le même code relatif marche en dev ET en prod, sans
 * URL absolue codée en dur (qui pointait `http://localhost:8080` et cassait en prod).
 */
export const API_BASE = '';
