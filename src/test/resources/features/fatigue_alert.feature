# Scénario métier clé (J4, BDD) : ingestion de relevés -> recalcul Spring Batch ->
# l'appareil passe (ou non) en alerte de maintenance. Voir dev.ynzi.fatiguetracker.cucumber
# pour les step definitions et dev.ynzi.fatiguetracker.fatigue.FatigueCalculator pour la
# formule illustrative (aucune donnée ni méthode propriétaire d'un employeur réel).
Feature: Recalcul de l'indice de fatigue et alerte de maintenance

  En tant que responsable maintenance, je veux que le recalcul de l'indice de fatigue
  de la flotte déclenche une alerte sur les appareils dont les relevés de vol dépassent
  le seuil configuré, pour anticiper une intervention.

  Scenario: Un appareil fortement sollicité passe en alerte après recalcul
    Given un appareil "F-BDD1" fraîchement enregistré, sans aucun relevé
    When des relevés de vol fortement sollicités sont ingérés pour l'appareil "F-BDD1"
    And le recalcul de l'indice de fatigue de la flotte est déclenché par un compte MAINT
    Then l'appareil "F-BDD1" est signalé en alerte de maintenance
    And l'indice de fatigue de l'appareil "F-BDD1" dépasse le seuil configuré

  Scenario: Un appareil peu sollicité ne déclenche pas d'alerte après recalcul
    Given un appareil "F-BDD5" fraîchement enregistré, sans aucun relevé
    When un relevé de vol léger est ingéré pour l'appareil "F-BDD5"
    And le recalcul de l'indice de fatigue de la flotte est déclenché par un compte MAINT
    Then l'appareil "F-BDD5" n'est pas en alerte de maintenance
