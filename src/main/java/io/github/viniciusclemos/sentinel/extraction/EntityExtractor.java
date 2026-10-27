package io.github.viniciusclemos.sentinel.extraction;

import io.github.viniciusclemos.sentinel.extraction.Entities.FoundUrl;
import java.net.IDN;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Pulls links, Pix keys, documents, phones and amounts out of a message.
 *
 * <p>Order matters: e-mails are taken out before links (or "joao@banco.com" would also be read as
 * the link "banco.com"), random Pix keys before documents, CNPJs before CPFs and phones last,
 * since a CNPJ contains something shaped like a phone number.
 */
@Component
public class EntityExtractor {

    // Scammers often write links without http ("itau-seguro.com/acesso") because chat apps still
    // make them clickable. Hosts may be Unicode on purpose (homograph attacks), hence \p{L}.
    private static final Pattern URL = Pattern.compile(
            "(?:https?://)?(?:[\\p{L}\\p{N}](?:[\\p{L}\\p{N}-]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}][\\p{L}\\p{N}-]{1,23}"
                    + "(?::\\d{2,5})?(?:/[^\\s<>\"')]*)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_URL = Pattern.compile(
            "https?://\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{2,5})?(?:/[^\\s<>\"')]*)?", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern RANDOM_KEY = Pattern.compile(
            "\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CNPJ = Pattern.compile("(?<!\\d)\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}(?!\\d)");
    private static final Pattern CPF = Pattern.compile("(?<!\\d)\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}(?!\\d)");
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?55\\s?)?\\(?\\d{2}\\)?\\s?9?\\s?\\d{4}[-\\s]?\\d{4}(?!\\d)");
    private static final Pattern AMOUNT = Pattern.compile("R\\$\\s?\\d{1,3}(?:\\.\\d{3})*(?:,\\d{2})?|R\\$\\s?\\d+(?:,\\d{2})?");

    // a bare "word.word" only counts as a link with a real ending, so "R$1.500" or "obs.: x" don't
    private static final Set<String> TLDS = Set.of(
            "com", "br", "net", "org", "gov", "app", "io", "co", "me", "info", "xyz", "top", "site", "online",
            "click", "link", "live", "shop", "store", "tk", "ml", "ga", "cf", "gq", "cc", "ly", "gd", "us", "biz",
            "vip", "icu", "buzz", "rest", "lat", "pro", "ws", "to", "sh", "page", "dev", "ru", "cn", "su");

    public Entities extract(String text) {
        List<String> emails = findAll(EMAIL, text);
        String rest = EMAIL.matcher(text).replaceAll(" ");

        List<FoundUrl> urls = new ArrayList<>();
        List<String> candidates = findAll(IP_URL, rest);
        candidates.addAll(findAll(URL, IP_URL.matcher(rest).replaceAll(" ")));
        Set<String> seenHosts = new LinkedHashSet<>();
        for (String candidate : candidates) {
            String raw = candidate.replaceAll("[.,;:!?]+$", "");
            String host = hostOf(raw);
            if (host.isEmpty()) {
                continue;
            }
            String tld = host.substring(host.lastIndexOf('.') + 1);
            boolean explicit = raw.toLowerCase(Locale.ROOT).startsWith("http");
            if (!explicit && !TLDS.contains(tld) && !tld.startsWith("xn--")) {
                continue;
            }
            if (seenHosts.add(host)) {
                urls.add(new FoundUrl(raw, host));
            }
        }

        List<String> randomKeys = findAll(RANDOM_KEY, rest).stream().map(k -> k.toLowerCase(Locale.ROOT)).distinct().toList();
        String noKeys = RANDOM_KEY.matcher(rest).replaceAll(" ");
        List<String> cnpjs = findAll(CNPJ, noKeys).stream().filter(BrazilianIds::isValidCnpj).distinct().toList();
        String noCnpj = CNPJ.matcher(noKeys).replaceAll(" ");
        List<String> cpfs = findAll(CPF, noCnpj).stream().filter(BrazilianIds::isValidCpf).distinct().toList();
        String noCpf = CPF.matcher(noCnpj).replaceAll(" ");
        List<String> phones = findAll(PHONE, noCpf).stream().map(String::strip).distinct().toList();
        List<String> amounts = findAll(AMOUNT, text).stream().distinct().toList();

        return new Entities(urls, emails.stream().distinct().toList(), cpfs, cnpjs, randomKeys, phones, amounts);
    }

    /**
     * Host of a link, lowercase, without "www.", and in Unicode even if it came as punycode, so
     * "xn--tau-2na.com" and "ıtau.com" are compared the same way later.
     */
    static String hostOf(String raw) {
        String url = raw.contains("://") ? raw : "http://" + raw;
        String host;
        try {
            String ascii = IDN.toASCII(authorityHost(url), IDN.ALLOW_UNASSIGNED);
            host = URI.create("http://" + ascii).getHost();
        } catch (IllegalArgumentException e) {
            return "";
        }
        if (host == null) {
            return "";
        }
        host = IDN.toUnicode(host.toLowerCase(Locale.ROOT), IDN.ALLOW_UNASSIGNED);
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private static String authorityHost(String url) {
        String afterScheme = url.substring(url.indexOf("://") + 3);
        int end = afterScheme.length();
        for (char c : new char[] {'/', '?', '#', ':'}) {
            int i = afterScheme.indexOf(c);
            if (i >= 0 && i < end) {
                end = i;
            }
        }
        return afterScheme.substring(0, end);
    }

    private static List<String> findAll(Pattern pattern, String text) {
        List<String> out = new ArrayList<>();
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }
}
