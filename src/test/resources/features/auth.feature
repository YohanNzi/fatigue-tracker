# Scénario métier clé (J4, BDD) : login -> JWT -> écriture autorisée / refusée selon le
# rôle. Voir dev.ynzi.fatiguetracker.cucumber pour les step definitions et
# dev.ynzi.fatiguetracker.security.SecurityConfig pour le modèle d'autorisation réel.
Feature: Authentification JWT et autorisation d'écriture

  La lecture de l'API est publique ; l'écriture (ex. création d'un appareil) exige un JWT
  valide portant le rôle MAINT, obtenu via login avec un compte de démo.

  Scenario: Un compte MAINT peut créer un appareil après connexion
    Given l'utilisateur se connecte avec le compte de démo "demo.maint" et le mot de passe "maint123"
    When il crée l'appareil "F-BDD10" avec son jeton
    Then la création de l'appareil "F-BDD10" réussit

  Scenario: Un compte VIEWER ne peut pas créer d'appareil
    Given l'utilisateur se connecte avec le compte de démo "demo.viewer" et le mot de passe "viewer123"
    When il crée l'appareil "F-BDD11" avec son jeton
    Then la création de l'appareil "F-BDD11" est refusée avec le statut 403

  Scenario: Une requête sans jeton ne peut pas créer d'appareil
    When il crée l'appareil "F-BDD12" sans jeton
    Then la création de l'appareil "F-BDD12" est refusée avec le statut 401
