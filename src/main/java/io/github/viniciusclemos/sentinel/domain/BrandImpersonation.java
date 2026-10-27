package io.github.viniciusclemos.sentinel.domain;

/**
 * A domain that looks like it's pretending to be a known brand.
 *
 * @param brand the brand it imitates
 * @param legitimateDomain the brand's real domain
 * @param analyzedDomain the domain that was checked
 * @param technique how the imitation was built
 * @param difference what exactly differs, in words ("'0' in place of 'o'")
 * @param confidence from 0 to 1, how sure the match is (not how dangerous the domain is)
 */
public record BrandImpersonation(
        String brand,
        String legitimateDomain,
        String analyzedDomain,
        Technique technique,
        String difference,
        double confidence) {

    public enum Technique {
        /** Letters from another alphabet that look Latin: "іtau.com" with a Cyrillic "і". */
        HOMOGRAPH,
        /** Digits or letter pairs that read as a letter: "g00gle.com", "rnicrosoft.com". */
        CHARACTER_SUBSTITUTION,
        /** A letter added, removed, swapped or replaced: "itauu.com", "itua.com". */
        TYPOSQUATTING,
        /** The real name plus extra words: "itau-seguranca.com". */
        COMBOSQUATTING,
        /** The real name under another ending: "itau.net" instead of "itau.com.br". */
        WRONG_TLD
    }
}
