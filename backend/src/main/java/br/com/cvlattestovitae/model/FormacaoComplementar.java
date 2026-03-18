package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class FormacaoComplementar {
    private String tipo;          // CURSO_CURTA_DURACAO, EXTENSAO_UNIVERSITARIA
    private String nomeCurso;
    private String nomeInstituicao;
    private String cargaHoraria;
    private String anoInicio;
    private String anoConclusao;
    private String statusCurso;
}
