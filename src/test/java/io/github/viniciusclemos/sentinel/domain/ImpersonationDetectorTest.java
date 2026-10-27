package io.github.viniciusclemos.sentinel.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.viniciusclemos.sentinel.domain.BrandImpersonation.Technique;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ImpersonationDetectorTest {

    private final ImpersonationDetector detector = new ImpersonationDetector();

    @ParameterizedTest
    @CsvSource({
        // host,                         brand,       technique
        "іtau.com.br,                    Itaú,        HOMOGRAPH",              // Cyrillic і
        "nubаnk.com,                     Nubank,      HOMOGRAPH",              // Cyrillic а
        "g00gle.com,                     Google,      CHARACTER_SUBSTITUTION",
        "rnicrosoft-login.com,           Microsoft,   CHARACTER_SUBSTITUTION",
        "1tau.com.br,                    Itaú,        CHARACTER_SUBSTITUTION",
        "itauu.com,                      Itaú,        TYPOSQUATTING",
        "santnader.com.br,               Santander,   TYPOSQUATTING",
        "itau-seguranca.com,             Itaú,        COMBOSQUATTING",
        "atendimento.nubank-app.online,  Nubank,      COMBOSQUATTING",
        "caixa-premios.site,             Caixa,       COMBOSQUATTING",
        "itau.net,                       Itaú,        WRONG_TLD",
    })
    void findsTheBrandAndTheTrick(String host, String brand, Technique technique) {
        BrandImpersonation found = detector.check(host).orElseThrow();
        assertThat(found.brand()).isEqualTo(brand);
        assertThat(found.technique()).isEqualTo(technique);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "itau.com.br", "internetbanking.itau.com.br", "www.bb.com.br", "nubank.com.br",
        "caixa.gov.br", "mercadolivre.com.br", "google.com",
        // ordinary words that contain or come close to a brand name
        "internet.com", "receitasdebolo.com", "caixadesom.com.br", "bbq-house.com", "metal.com",
        "185.12.33.4",
    })
    void leavesRealAndUnrelatedDomainsAlone(String host) {
        assertThat(detector.check(host)).isEmpty();
    }

    @Test
    void saysWhatIsDifferent() {
        assertThat(detector.check("g00gle.com").orElseThrow().difference())
                .isEqualTo("'0' in place of 'o', '0' in place of 'o' (g00gle reads as google)");
        assertThat(detector.check("itauu.com").orElseThrow().difference()).startsWith("extra letter 'u'");
        assertThat(detector.check("santnader.com.br").orElseThrow().difference()).startsWith("letters 'a' and 'n' swapped");
        assertThat(detector.check("іtau.com.br").orElseThrow().difference()).contains("U+0456", "cyrillic");
    }

    @Test
    void gluedNamesAreLessCertainThanHyphenatedOnes() {
        double hyphen = detector.check("itau-seguranca.com").orElseThrow().confidence();
        double glued = detector.check("amazonas-turismo.com").orElseThrow().confidence();
        assertThat(glued).isLessThan(hyphen);
    }

    @Test
    void editDistanceCountsAdjacentSwapsAsOne() {
        assertThat(ImpersonationDetector.editDistance("itua", "itau")).isEqualTo(1);
        assertThat(ImpersonationDetector.editDistance("bradesco", "bradesco")).isZero();
    }
}
