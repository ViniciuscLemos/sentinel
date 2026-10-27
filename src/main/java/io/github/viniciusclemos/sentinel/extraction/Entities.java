package io.github.viniciusclemos.sentinel.extraction;

import java.util.List;

/**
 * Everything worth checking that was found in a message.
 *
 * <p>Each list keeps the text exactly as it appeared, so the result can point back at the part of
 * the message (or of the screenshot) it came from.
 */
public record Entities(
        List<FoundUrl> urls,
        List<String> emails,
        List<String> cpfs,
        List<String> cnpjs,
        List<String> randomPixKeys,
        List<String> phones,
        List<String> amounts) {

    /** A link and its host, lowercase and without "www.". */
    public record FoundUrl(String raw, String host) {}

    public boolean hasPaymentData() {
        return !cpfs.isEmpty() || !cnpjs.isEmpty() || !randomPixKeys.isEmpty() || !amounts.isEmpty();
    }
}
