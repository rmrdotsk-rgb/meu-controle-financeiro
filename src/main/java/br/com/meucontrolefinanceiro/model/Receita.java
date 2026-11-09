package br.com.meucontrolefinanceiro.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "receitas")
public class Receita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe a descrição")
    @Size(min = 3, max = 120, message = "A descrição deve ter entre 3 e 120 caracteres")
    @Column(nullable = false, length = 120)
    private String descricao;

    @NotNull(message = "Informe o valor")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "Informe um valor com até 2 casas decimais")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @NotBlank(message = "Escolha uma categoria")
    @Pattern(regexp = "Salário|Freelance|Vendas|Outros", message = "Escolha uma categoria válida")
    @Column(nullable = false, length = 30)
    private String categoria;

    @NotNull(message = "Informe a data")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(nullable = false)
    private LocalDate data;

    public Receita() {}
    public Receita(String descricao, BigDecimal valor, String categoria, LocalDate data) {
        setDescricao(descricao); this.valor = valor; this.categoria = categoria; this.data = data;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao == null ? null : descricao.strip(); }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}
