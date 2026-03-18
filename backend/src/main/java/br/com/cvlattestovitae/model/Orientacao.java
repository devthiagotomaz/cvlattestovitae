package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class Orientacao {
    private String tipo;           // DOUTORADO, MESTRADO, INICIACAO_CIENTIFICA, etc.
    private String titulo;
    private String nomeOrientando;
    private String nomeInstituicao;
    private String anoInicio;
    private String anoConclusao;
    private String situacao;       // EM_ANDAMENTO, CONCLUIDA
}
