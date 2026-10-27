package io.github.viniciusclemos.sentinel.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Banks, stores and services that scams in Brazil pretend to be, with the domains they really use.
 *
 * <p>The token is the name as it shows up inside fake domains ("itau" in "itau-seguranca.com").
 * Tokens that are also ordinary words, or parts of them ("inter" in "internet", "caixa" in
 * "caixadesom"), are marked {@code wholePieceOnly}: they only count as a separate piece of the
 * domain, like "caixa-premios.com".
 */
public final class KnownBrands {

    public record Brand(String name, String token, Set<String> officialDomains, boolean wholePieceOnly) {
        public String mainDomain() {
            return officialDomains.iterator().next();
        }
    }

    public static final List<Brand> ALL = List.of(
            new Brand("Itaú", "itau", Set.of("itau.com.br"), false),
            new Brand("Bradesco", "bradesco", Set.of("bradesco.com.br"), false),
            new Brand("Santander", "santander", Set.of("santander.com.br"), false),
            new Brand("Caixa", "caixa", Set.of("caixa.gov.br"), true),
            new Brand("Banco do Brasil", "bb", Set.of("bb.com.br"), true),
            new Brand("Nubank", "nubank", Set.of("nubank.com.br", "nu.com.br"), false),
            new Brand("Inter", "inter", Set.of("bancointer.com.br", "inter.co"), true),
            new Brand("C6 Bank", "c6bank", Set.of("c6bank.com.br"), false),
            new Brand("PicPay", "picpay", Set.of("picpay.com"), false),
            new Brand("Mercado Pago", "mercadopago", Set.of("mercadopago.com.br"), false),
            new Brand("Mercado Livre", "mercadolivre", Set.of("mercadolivre.com.br"), false),
            new Brand("PagBank", "pagbank", Set.of("pagbank.com.br", "pagseguro.uol.com.br"), false),
            new Brand("Sicoob", "sicoob", Set.of("sicoob.com.br"), false),
            new Brand("Sicredi", "sicredi", Set.of("sicredi.com.br"), false),
            new Brand("Correios", "correios", Set.of("correios.com.br"), false),
            new Brand("gov.br", "govbr", Set.of("gov.br"), true),
            new Brand("Serasa", "serasa", Set.of("serasa.com.br"), false),
            new Brand("Shopee", "shopee", Set.of("shopee.com.br"), false),
            new Brand("Amazon", "amazon", Set.of("amazon.com.br", "amazon.com"), false),
            new Brand("Magalu", "magalu", Set.of("magazineluiza.com.br", "magalu.com"), false),
            new Brand("Google", "google", Set.of("google.com", "google.com.br"), false),
            new Brand("Microsoft", "microsoft", Set.of("microsoft.com"), false),
            new Brand("WhatsApp", "whatsapp", Set.of("whatsapp.com", "wa.me"), false),
            new Brand("Instagram", "instagram", Set.of("instagram.com"), false),
            new Brand("Netflix", "netflix", Set.of("netflix.com"), false));

    private KnownBrands() {}

    /** The brand that owns this registrable domain, if any. */
    public static Optional<Brand> owning(String registrableDomain) {
        return ALL.stream().filter(b -> b.officialDomains().contains(registrableDomain)).findFirst();
    }
}
