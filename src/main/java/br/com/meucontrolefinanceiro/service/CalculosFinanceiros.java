package br.com.meucontrolefinanceiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculosFinanceiros {
    private CalculosFinanceiros() {}

    public static BigDecimal saldo(BigDecimal receitas, BigDecimal despesas) {
        return receitas.subtract(despesas);
    }

    public static BigDecimal variacaoPercentual(BigDecimal atual, BigDecimal anterior) {
        if (anterior.signum() <= 0) return null;
        return atual.subtract(anterior).multiply(BigDecimal.valueOf(100))
                .divide(anterior, 1, RoundingMode.HALF_UP);
    }

    public static int percentualLimitado(BigDecimal atual, BigDecimal objetivo) {
        if (objetivo.signum() <= 0) throw new IllegalArgumentException("O objetivo deve ser positivo");
        if (atual.signum() <= 0) return 0;
        return atual.multiply(BigDecimal.valueOf(100))
                .divide(objetivo, 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100)).intValue();
    }
}
