package io.github.viniciusclemos.sentinel.extraction;

/** Check digits for CPF and CNPJ, so random digit sequences aren't taken for documents. */
public final class BrazilianIds {

    private BrazilianIds() {}

    public static String digits(String s) {
        return s.replaceAll("\\D", "");
    }

    public static boolean isValidCpf(String cpf) {
        String d = digits(cpf);
        if (d.length() != 11 || d.chars().distinct().count() == 1) {
            return false;
        }
        for (int n = 9; n <= 10; n++) {
            int total = 0;
            for (int i = 0; i < n; i++) {
                total += (d.charAt(i) - '0') * (n + 1 - i);
            }
            if ((total * 10 % 11) % 10 != d.charAt(n) - '0') {
                return false;
            }
        }
        return true;
    }

    private static final int[] CNPJ_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public static boolean isValidCnpj(String cnpj) {
        String d = digits(cnpj);
        if (d.length() != 14 || d.chars().distinct().count() == 1) {
            return false;
        }
        for (int n = 12; n <= 13; n++) {
            int offset = 13 - n;
            int total = 0;
            for (int i = 0; i < n; i++) {
                total += (d.charAt(i) - '0') * CNPJ_WEIGHTS[offset + i];
            }
            int check = total % 11 < 2 ? 0 : 11 - total % 11;
            if (check != d.charAt(n) - '0') {
                return false;
            }
        }
        return true;
    }
}
