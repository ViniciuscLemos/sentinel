package io.github.viniciusclemos.sentinel.domain;

import java.util.Locale;
import java.util.Set;

/** Splits hosts into the parts that matter when comparing them with brands. */
public final class DomainNames {

    // second-level domains under .br where the registered name is the third label (itau.com.br)
    private static final Set<String> BR_SECOND_LEVEL = Set.of(
            "com", "gov", "net", "org", "edu", "app", "art", "blog", "eco", "ind", "log", "seg");

    private DomainNames() {}

    /** The part someone actually registers: "login.itau.com.br" becomes "itau.com.br". */
    public static String registrable(String host) {
        String[] labels = host.toLowerCase(Locale.ROOT).replaceAll("^\\.+|\\.+$", "").split("\\.");
        int n = labels.length;
        if (n >= 3 && labels[n - 1].equals("br") && BR_SECOND_LEVEL.contains(labels[n - 2])) {
            return String.join(".", labels[n - 3], labels[n - 2], labels[n - 1]);
        }
        return n >= 2 ? labels[n - 2] + "." + labels[n - 1] : host;
    }

    /** The name part of the registrable domain: "itau-seguranca.com" becomes "itau-seguranca". */
    public static String nameLabel(String host) {
        String reg = registrable(host);
        int dot = reg.indexOf('.');
        return dot < 0 ? reg : reg.substring(0, dot);
    }

    public static boolean isIpAddress(String host) {
        return host.matches("\\d{1,3}(\\.\\d{1,3}){3}");
    }
}
