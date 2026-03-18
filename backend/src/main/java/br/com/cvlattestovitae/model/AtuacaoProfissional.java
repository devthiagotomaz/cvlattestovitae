package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class AtuacaoProfissional {
    private String nomeInstituicao;
    private String tipoVinculo;
    private String descricao;
    private String anoInicio;
    private String mesInicio;
    private String anoFim;
    private String mesFim;
}
