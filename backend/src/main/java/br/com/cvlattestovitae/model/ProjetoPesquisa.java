package br.com.cvlattestovitae.model;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class ProjetoPesquisa {
    private String titulo;
    private String descricao;
    private String nomeOrgao;
    private String anoInicio;
    private String anoFim;
    private List<String> linhasPesquisa = new ArrayList<>();
    private String situacao;
}
