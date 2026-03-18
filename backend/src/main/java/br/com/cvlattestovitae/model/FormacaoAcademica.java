package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class FormacaoAcademica {
    private String tipo;          // Doutorado, Mestrado, Graduação, etc.
    private String nomeCurso;
    private String nomeInstituicao;
    private String anoInicio;
    private String anoConclusao;
    private String statusCurso;
    private String tituloDissertacao;
    private String nomeOrientador;
}
