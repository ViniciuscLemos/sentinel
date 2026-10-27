package io.github.viniciusclemos.sentinel.domain;

import io.github.viniciusclemos.sentinel.domain.BrandImpersonation.Technique;
import io.github.viniciusclemos.sentinel.domain.KnownBrands.Brand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Tells whether a domain is dressed up as a known brand, and how.
 *
 * <p>Checks run from the most specific trick to the loosest one, and the most confident match
 * wins. Nothing here calls out to the network or to a model: it's string work on the domain name,
 * so the same domain always gets the same answer and every answer can say exactly what differs.
 */
@Component
public class ImpersonationDetector {

    public Optional<BrandImpersonation> check(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        if (DomainNames.isIpAddress(h) || isOfficial(h)) {
            return Optional.empty();
        }
        String label = DomainNames.nameLabel(h);
        String registrable = DomainNames.registrable(h);

        List<BrandImpersonation> found = new ArrayList<>();
        for (Brand brand : KnownBrands.ALL) {
            homograph(brand, label, registrable).ifPresent(found::add);
            sameNameOrExtraWords(brand, label, registrable).ifPresent(found::add);
            substitution(brand, label, registrable).ifPresent(found::add);
            typo(brand, label, registrable).ifPresent(found::add);
        }
        return found.stream().max(Comparator.comparingDouble(BrandImpersonation::confidence));
    }

    /** True for the brand's own domains and their subdomains ("login.itau.com.br"). */
    public static boolean isOfficial(String host) {
        return KnownBrands.ALL.stream()
                .flatMap(b -> b.officialDomains().stream())
                .anyMatch(d -> host.equals(d) || host.endsWith("." + d));
    }

    private Optional<BrandImpersonation> homograph(Brand brand, String label, String registrable) {
        if (!Confusables.hasNonAscii(label)) {
            return Optional.empty();
        }
        String skeleton = Confusables.latinSkeleton(label);
        if (!contains(skeleton, brand)) {
            return Optional.empty();
        }
        List<String> odd = new ArrayList<>();
        for (char c : label.toCharArray()) {
            if (c > 127 && c != '-') {
                odd.add("'" + c + "' (U+" + String.format("%04X", (int) c) + ", "
                        + Character.UnicodeScript.of(c).name().toLowerCase(Locale.ROOT) + ")");
            }
        }
        return Optional.of(new BrandImpersonation(brand.name(), brand.mainDomain(), registrable, Technique.HOMOGRAPH,
                "looks like '" + skeleton + "' but uses " + String.join(", ", odd), 0.95));
    }

    private Optional<BrandImpersonation> sameNameOrExtraWords(Brand brand, String label, String registrable) {
        if (Confusables.hasNonAscii(label)) {
            return Optional.empty();
        }
        if (label.equals(brand.token())) {
            String ending = registrable.substring(label.length());
            return Optional.of(new BrandImpersonation(brand.name(), brand.mainDomain(), registrable, Technique.WRONG_TLD,
                    "same name under '" + ending + "' instead of the official domain", 0.9));
        }
        if (contains(label, brand)) {
            String extra = label.replace(brand.token(), " ").replaceAll("[-_]", " ").strip().replaceAll("\\s+", " ");
            // "itau-seguranca" is clearer than "itauseguranca"; glued names can also be innocent
            // words that happen to contain the brand ("amazonas"), so they get less confidence
            boolean wholePiece = Arrays.asList(pieces(label)).contains(brand.token());
            return Optional.of(new BrandImpersonation(brand.name(), brand.mainDomain(), registrable,
                    Technique.COMBOSQUATTING, "the real name plus '" + extra + "'", wholePiece ? 0.85 : 0.7));
        }
        return Optional.empty();
    }

    private Optional<BrandImpersonation> substitution(Brand brand, String label, String registrable) {
        if (brand.wholePieceOnly() || contains(label, brand)) {
            return Optional.empty();
        }
        for (String skeleton : Confusables.visualSkeletons(label)) {
            if (contains(skeleton, brand) && !skeleton.equals(label)) {
                return Optional.of(new BrandImpersonation(brand.name(), brand.mainDomain(), registrable,
                        Technique.CHARACTER_SUBSTITUTION, describeSwaps(label, skeleton), 0.9));
            }
        }
        return Optional.empty();
    }

    private Optional<BrandImpersonation> typo(Brand brand, String label, String registrable) {
        if (brand.wholePieceOnly() || Confusables.hasNonAscii(label)) {
            return Optional.empty();
        }
        String token = brand.token();
        // a 4-letter name is one typo away from lots of real words; longer names can take two
        int maxDistance = token.length() >= 8 ? 2 : 1;
        for (String piece : pieces(label)) {
            if (piece.length() < 4 || piece.equals(token)) {
                continue;
            }
            int distance = editDistance(piece, token);
            if (distance >= 1 && distance <= maxDistance) {
                double confidence = distance == 1 ? 0.8 : 0.6;
                return Optional.of(new BrandImpersonation(brand.name(), brand.mainDomain(), registrable,
                        Technique.TYPOSQUATTING, describeEdit(piece, token), confidence));
            }
        }
        return Optional.empty();
    }

    private static boolean contains(String text, Brand brand) {
        if (Arrays.asList(pieces(text)).contains(brand.token())) {
            return true;
        }
        return !brand.wholePieceOnly() && text.replaceAll("[-_]", "").contains(brand.token());
    }

    private static String[] pieces(String label) {
        return Arrays.stream(label.split("[-_]")).filter(p -> !p.isEmpty()).toArray(String[]::new);
    }

    /** Optimal string alignment distance: insertions, deletions, substitutions and adjacent swaps. */
    static int editDistance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && a.charAt(i - 1) == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[a.length()][b.length()];
    }

    /** Puts a one-edit difference into words; anything bigger is just reported as a distance. */
    static String describeEdit(String seen, String real) {
        if (seen.length() == real.length() + 1) {
            for (int i = 0; i < seen.length(); i++) {
                if ((seen.substring(0, i) + seen.substring(i + 1)).equals(real)) {
                    return "extra letter '" + seen.charAt(i) + "' (" + seen + " vs " + real + ")";
                }
            }
        }
        if (seen.length() + 1 == real.length()) {
            for (int i = 0; i < real.length(); i++) {
                if ((real.substring(0, i) + real.substring(i + 1)).equals(seen)) {
                    return "missing letter '" + real.charAt(i) + "' (" + seen + " vs " + real + ")";
                }
            }
        }
        if (seen.length() == real.length()) {
            List<Integer> diff = new ArrayList<>();
            for (int i = 0; i < seen.length(); i++) {
                if (seen.charAt(i) != real.charAt(i)) diff.add(i);
            }
            if (diff.size() == 1) {
                int i = diff.get(0);
                return "'" + seen.charAt(i) + "' in place of '" + real.charAt(i) + "' (" + seen + " vs " + real + ")";
            }
            if (diff.size() == 2 && diff.get(1) == diff.get(0) + 1
                    && seen.charAt(diff.get(0)) == real.charAt(diff.get(1))
                    && seen.charAt(diff.get(1)) == real.charAt(diff.get(0))) {
                return "letters '" + real.charAt(diff.get(0)) + "' and '" + real.charAt(diff.get(1))
                        + "' swapped (" + seen + " vs " + real + ")";
            }
        }
        return editDistance(seen, real) + " letters different (" + seen + " vs " + real + ")";
    }

    private static String describeSwaps(String label, String skeleton) {
        if (label.length() == skeleton.length()) {
            List<String> swaps = new ArrayList<>();
            for (int i = 0; i < label.length(); i++) {
                if (label.charAt(i) != skeleton.charAt(i)) {
                    swaps.add("'" + label.charAt(i) + "' in place of '" + skeleton.charAt(i) + "'");
                }
            }
            return String.join(", ", swaps) + " (" + label + " reads as " + skeleton + ")";
        }
        return label + " reads as " + skeleton;
    }
}
