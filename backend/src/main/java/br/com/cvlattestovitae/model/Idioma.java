package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class Idioma {
    private String nome;
    private String proficienciaConversacao;
    private String proficienciaLeitura;
    private String proficienciaEscrita;
    private String proficienciaCompreensao;
}
