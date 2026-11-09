package br.com.meucontrolefinanceiro.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DemonstracaoInfo {
    private final boolean habilitada;
    private final String email;

    public DemonstracaoInfo(@Value("${app.demo.enabled:false}") boolean habilitada,
            @Value("${app.demo.email:demo@meucontrole.local}") String email) {
        this.habilitada = habilitada;
        this.email = email;
    }

    @ModelAttribute("demoEnabled")
    public boolean habilitada() { return habilitada; }

    @ModelAttribute("demoEmail")
    public String email() { return habilitada ? email : ""; }
}
