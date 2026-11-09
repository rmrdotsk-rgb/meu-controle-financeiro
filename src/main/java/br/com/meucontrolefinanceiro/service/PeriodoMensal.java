package br.com.meucontrolefinanceiro.service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** Mantém o mês compatível com os formulários HTML e a coluna AAAA-MM. */
public final class PeriodoMensal {
    private PeriodoMensal() {}

    public static YearMonth interpretar(String valor) {
        if (valor == null || !valor.matches("(?!0000)[0-9]{4}-(0[1-9]|1[0-2])")) {
            throw new DateTimeParseException("Informe um mês entre 0001-01 e 9999-12", valor == null ? "" : valor, 0);
        }
        return YearMonth.parse(valor);
    }
}
