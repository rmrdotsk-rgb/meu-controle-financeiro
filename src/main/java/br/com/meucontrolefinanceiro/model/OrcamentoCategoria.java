package br.com.meucontrolefinanceiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "orcamentos", uniqueConstraints = @UniqueConstraint(name = "uk_orcamento_mes_categoria", columnNames = {"mes", "categoria"}))
public class OrcamentoCategoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o mês")
    @Pattern(regexp = "(?!0000)[0-9]{4}-(0[1-9]|1[0-2])", message = "Informe um mês válido")
    @Column(nullable = false, length = 7)
    private String mes;

    @NotBlank(message = "Escolha uma categoria")
    @Pattern(regexp = "Alimentação|Educação|Transporte|Casa|Saúde|Lazer|Outros", message = "Escolha uma categoria válida")
    @Column(nullable = false, length = 30)
    private String categoria;

    @NotNull(message = "Informe o limite")
    @DecimalMin(value = "0.01", message = "O limite deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "Informe um valor com até 2 casas decimais")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal limite;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMes() { return mes; }
    public void setMes(String mes) { this.mes = mes; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public BigDecimal getLimite() { return limite; }
    public void setLimite(BigDecimal limite) { this.limite = limite; }
}
