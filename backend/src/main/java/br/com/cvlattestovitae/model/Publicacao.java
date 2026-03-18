package br.com.cvlattestovitae.model;

import lombok.Data;

@Data
public class Publicacao {
    private String tipo;    // ARTIGO, LIVRO, CAPITULO
    private String titulo;
    private String autores;
    private String veiculo;  // Periódico, título do livro, etc.
    private String ano;
    private String doi;
    private String volume;
    private String numero;
    private String paginas;
    private String editora;
    private String cidade;
}
