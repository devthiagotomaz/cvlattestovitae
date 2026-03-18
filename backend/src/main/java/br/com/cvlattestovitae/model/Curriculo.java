package br.com.cvlattestovitae.model;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class Curriculo {

    // Dados Pessoais
    private String nomeCompleto;
    private String nomeCitacoes;
    private String lattesId;
    private String dataNascimento;
    private String nacionalidade;
    private String orcid;
    private String email;
    private String homePage;
    private String resumo;

    // Endereço profissional
    private String nomeInstituicaoEndereco;
    private String cidadeEndereco;
    private String ufEndereco;
    private String paisEndereco;

    // Seções
    private List<FormacaoAcademica> formacoes = new ArrayList<>();
    private List<AtuacaoProfissional> atuacoes = new ArrayList<>();
    private List<Publicacao> artigosPublicados = new ArrayList<>();
    private List<Publicacao> livros = new ArrayList<>();
    private List<Publicacao> capitulosLivro = new ArrayList<>();
    private List<ProjetoPesquisa> projetos = new ArrayList<>();
    private List<Orientacao> orientacoes = new ArrayList<>();
    private List<Premio> premios = new ArrayList<>();
    private List<Idioma> idiomas = new ArrayList<>();
    private List<AreaAtuacao> areasAtuacao = new ArrayList<>();
    private List<FormacaoComplementar> formacoesComplementares = new ArrayList<>();
}
