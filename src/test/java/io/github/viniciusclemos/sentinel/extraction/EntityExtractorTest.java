package io.github.viniciusclemos.sentinel.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.viniciusclemos.sentinel.extraction.Entities.FoundUrl;
import org.junit.jupiter.api.Test;

class EntityExtractorTest {

    private final EntityExtractor extractor = new EntityExtractor();

    @Test
    void linksWithAndWithoutHttp() {
        Entities e = extractor.extract("Acesse itau-seguranca.com/token agora ou https://bit.ly/3xYz");
        assertThat(e.urls()).extracting(FoundUrl::host).containsExactly("itau-seguranca.com", "bit.ly");
    }

    @Test
    void numbersAndAbbreviationsAreNotLinks() {
        assertThat(extractor.extract("Valor de R$1.500,00. Obs.: pagar até amanhã. Versão 2.0").urls()).isEmpty();
    }

    @Test
    void linkWithAnIpAddress() {
        assertThat(extractor.extract("entre em http://185.12.33.4/login pra confirmar").urls())
                .extracting(FoundUrl::host).containsExactly("185.12.33.4");
    }

    @Test
    void punycodeAndUnicodeHostsEndUpTheSame() {
        // "ıtau" with a dotless i, written both ways
        assertThat(extractor.extract("http://xn--tau-iua.com/").urls().get(0).host()).isEqualTo("ıtau.com");
        assertThat(extractor.extract("ıtau.com/login").urls().get(0).host()).isEqualTo("ıtau.com");
    }

    @Test
    void emailIsNotReadAsALink() {
        Entities e = extractor.extract("manda o comprovante pra suporte@banco-digital.net");
        assertThat(e.emails()).containsExactly("suporte@banco-digital.net");
        assertThat(e.urls()).isEmpty();
    }

    @Test
    void pixKeysAndDocuments() {
        Entities e = extractor.extract(
                "chave pix 529.982.247-25 ou CNPJ 11.222.333/0001-81 ou 123e4567-e89b-12d3-a456-426614174000");
        assertThat(e.cpfs()).containsExactly("529.982.247-25");
        assertThat(e.cnpjs()).containsExactly("11.222.333/0001-81");
        assertThat(e.randomPixKeys()).containsExactly("123e4567-e89b-12d3-a456-426614174000");
        assertThat(e.hasPaymentData()).isTrue();
    }

    @Test
    void invalidCpfIsIgnored() {
        assertThat(extractor.extract("cpf 111.111.111-11 e 123.456.789-00").cpfs()).isEmpty();
    }

    @Test
    void phoneAndAmount() {
        Entities e = extractor.extract("me liga no (11) 98765-4321, é só R$ 2.300,00");
        assertThat(e.phones()).containsExactly("(11) 98765-4321");
        assertThat(e.amounts()).containsExactly("R$ 2.300,00");
    }

    @Test
    void checkDigits() {
        assertThat(BrazilianIds.isValidCpf("52998224725")).isTrue();
        assertThat(BrazilianIds.isValidCpf("52998224724")).isFalse();
        assertThat(BrazilianIds.isValidCnpj("11222333000181")).isTrue();
        assertThat(BrazilianIds.isValidCnpj("11222333000180")).isFalse();
    }
}
