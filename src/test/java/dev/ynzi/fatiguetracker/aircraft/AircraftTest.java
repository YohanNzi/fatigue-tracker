package dev.ynzi.fatiguetracker.aircraft;

import dev.ynzi.fatiguetracker.common.DomainRuleViolationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariants de l'appareil, à la création comme au remplacement complet (PUT). */
class AircraftTest {

    @Test
    void replaceDetails_updatesAllCharacteristics() {
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);

        aircraft.replaceDetails("F-WXYZ", "Rafale", 1350.0);

        assertThat(aircraft.getRegistration()).isEqualTo("F-WXYZ");
        assertThat(aircraft.getModel()).isEqualTo("Rafale");
        assertThat(aircraft.getFlightHours()).isEqualTo(1350.0);
    }

    @Test
    void blankRegistration_isRejected() {
        assertThatThrownBy(() -> new Aircraft(" ", "A320", 10.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("immatriculation");
    }

    @Test
    void negativeFlightHours_areRejected() {
        assertThatThrownBy(() -> new Aircraft("F-ABCD", "A320", -1.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("heures de vol");
    }

    /** Un remplacement invalide ne laisse pas l'appareil dans un état partiellement modifié. */
    @Test
    void invalidReplacement_leavesAircraftUnchanged() {
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);

        assertThatThrownBy(() -> aircraft.replaceDetails("F-WXYZ", "", 1350.0))
                .isInstanceOf(DomainRuleViolationException.class);

        assertThat(aircraft.getRegistration()).isEqualTo("F-ABCD");
    }
}
