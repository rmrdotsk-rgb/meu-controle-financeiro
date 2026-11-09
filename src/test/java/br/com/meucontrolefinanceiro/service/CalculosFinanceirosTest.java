package br.com.meucontrolefinanceiro.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CalculosFinanceirosTest {
    @Test void saldoComValoresExatos() {
        assertEquals(new BigDecimal("1700.00"),
                CalculosFinanceiros.saldo(new BigDecimal("3000.00"), new BigDecimal("1300.00")));
    }
    @Test void aumentoDeVinteECincoPorCento() {
        assertEquals(new BigDecimal("25.0"),
                CalculosFinanceiros.variacaoPercentual(new BigDecimal("250"), new BigDecimal("200")));
    }
    @Test void semBaseNoMesAnterior() {
        assertNull(CalculosFinanceiros.variacaoPercentual(new BigDecimal("250"), BigDecimal.ZERO));
    }
    @Test void metaComAportesCorretos() {
        assertEquals(25, CalculosFinanceiros.percentualLimitado(new BigDecimal("500"), new BigDecimal("2000")));
    }
    @Test void progressoNaoUltrapassaCem() {
        assertEquals(100, CalculosFinanceiros.percentualLimitado(new BigDecimal("4000"), new BigDecimal("2000")));
    }
    @Test void naoPermiteObjetivoZero() {
        assertThrows(IllegalArgumentException.class, () ->
                CalculosFinanceiros.percentualLimitado(BigDecimal.ONE, BigDecimal.ZERO));
    }
}
