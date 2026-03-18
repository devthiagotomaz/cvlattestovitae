package br.com.cvlattestovitae.service;

import br.com.cvlattestovitae.model.*;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a CNPq Lattes XML file and maps it to a {@link Curriculo} model.
 */
@Service
public class LattesParserService {

    public Curriculo parse(InputStream xmlInputStream)
            throws ParserConfigurationException, SAXException, IOException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Disable external entities to prevent XXE attacks
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlInputStream);
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();
        if (!"CURRICULO-VITAE".equals(root.getNodeName())) {
            throw new IllegalArgumentException(
                    "Arquivo inválido: o elemento raiz deve ser CURRICULO-VITAE. " +
                    "Certifique-se de que está enviando um XML exportado da Plataforma Lattes.");
        }

        Curriculo cv = new Curriculo();
        cv.setLattesId(root.getAttribute("NUMERO-IDENTIFICADOR"));

        parseDadosGerais(cv, root);
        parseFormacaoAcademica(cv, root);
        parseFormacaoComplementar(cv, root);
        parseAtuacoesProfissionais(cv, root);
        parseProducaoBibliografica(cv, root);
        parseProjetosPesquisa(cv, root);
        parseOrientacoes(cv, root);
        parsePremios(cv, root);
        parseIdiomas(cv, root);
        parseAreasAtuacao(cv, root);

        return cv;
    }

    // -------------------------------------------------------------------------
    // DADOS GERAIS
    // -------------------------------------------------------------------------
    private void parseDadosGerais(Curriculo cv, Element root) {
        NodeList list = root.getElementsByTagName("DADOS-GERAIS");
        if (list.getLength() == 0) return;
        Element dg = (Element) list.item(0);

        cv.setNomeCompleto(dg.getAttribute("NOME-COMPLETO"));
        cv.setNomeCitacoes(dg.getAttribute("NOME-EM-CITACOES-BIBLIOGRAFICAS"));
        cv.setNacionalidade(dg.getAttribute("PAIS-DE-NASCIMENTO"));
        cv.setOrcid(dg.getAttribute("ORCID-ID"));

        String dataNasc = dg.getAttribute("DATA-NASCIMENTO");
        if (dataNasc != null && dataNasc.length() == 8) {
            // Format DDMMYYYY → DD/MM/YYYY
            cv.setDataNascimento(dataNasc.substring(0, 2) + "/" +
                    dataNasc.substring(2, 4) + "/" + dataNasc.substring(4));
        }

        // Resumo CV
        NodeList resumoList = dg.getElementsByTagName("RESUMO-CV");
        if (resumoList.getLength() > 0) {
            Element resumo = (Element) resumoList.item(0);
            String texto = resumo.getAttribute("TEXTO-RESUMO-CV-RH");
            if (texto == null || texto.isBlank()) {
                texto = resumo.getAttribute("TEXTO-RESUMO-CV-RH-EN");
            }
            cv.setResumo(texto);
        }

        // Endereço profissional
        NodeList endList = dg.getElementsByTagName("ENDERECO-PROFISSIONAL");
        if (endList.getLength() > 0) {
            Element end = (Element) endList.item(0);
            cv.setEmail(end.getAttribute("E-MAIL"));
            cv.setHomePage(end.getAttribute("HOME-PAGE"));
            cv.setNomeInstituicaoEndereco(end.getAttribute("NOME-INSTITUICAO-EMPRESA"));
            cv.setCidadeEndereco(end.getAttribute("CIDADE"));
            cv.setUfEndereco(end.getAttribute("UF"));
            cv.setPaisEndereco(end.getAttribute("PAIS"));
        } else {
            // Fallback: try residential email
            NodeList endRes = dg.getElementsByTagName("ENDERECO-RESIDENCIAL");
            if (endRes.getLength() > 0) {
                cv.setEmail(((Element) endRes.item(0)).getAttribute("E-MAIL"));
            }
        }
    }

    // -------------------------------------------------------------------------
    // FORMAÇÃO ACADÊMICA
    // -------------------------------------------------------------------------
    private void parseFormacaoAcademica(Curriculo cv, Element root) {
        NodeList secList = root.getElementsByTagName("FORMACAO-ACADEMICA-TITULACAO");
        if (secList.getLength() == 0) return;
        Element sec = (Element) secList.item(0);

        String[] tipos = {"DOUTORADO", "MESTRADO", "MESTRADO-PROFISSIONAL",
                "ESPECIALIZACAO", "GRADUACAO", "APERFEICOAMENTO",
                "LIVRE-DOCENCIA", "RESIDENCIA-MEDICA"};

        for (String tipo : tipos) {
            NodeList nodes = sec.getElementsByTagName(tipo);
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                FormacaoAcademica f = new FormacaoAcademica();
                f.setTipo(labelFormacao(tipo));
                f.setNomeCurso(firstNonBlank(
                        el.getAttribute("NOME-CURSO"),
                        el.getAttribute("NOME-CURSO-AREA-BASICA")));
                f.setNomeInstituicao(el.getAttribute("NOME-INSTITUICAO"));
                f.setAnoInicio(el.getAttribute("ANO-DE-INICIO"));
                f.setAnoConclusao(el.getAttribute("ANO-DE-CONCLUSAO"));
                f.setStatusCurso(el.getAttribute("STATUS-DO-CURSO"));
                f.setTituloDissertacao(firstNonBlank(
                        el.getAttribute("TITULO-DA-DISSERTACAO"),
                        el.getAttribute("TITULO-DA-TESE"),
                        el.getAttribute("TITULO-DO-TRABALHO-DE-CONCLUSAO-DE-CURSO"),
                        el.getAttribute("TITULO-DA-MONOGRAFIA")));
                f.setNomeOrientador(firstNonBlank(
                        el.getAttribute("NOME-DO-ORIENTADOR"),
                        el.getAttribute("NOME-DO-ORIENTADOR-PRINCIPAL")));
                cv.getFormacoes().add(f);
            }
        }
    }

    private String labelFormacao(String tipo) {
        return switch (tipo) {
            case "DOUTORADO" -> "Doutorado";
            case "MESTRADO" -> "Mestrado";
            case "MESTRADO-PROFISSIONAL" -> "Mestrado Profissional";
            case "ESPECIALIZACAO" -> "Especialização";
            case "GRADUACAO" -> "Graduação";
            case "APERFEICOAMENTO" -> "Aperfeiçoamento / Extensão";
            case "LIVRE-DOCENCIA" -> "Livre-Docência";
            case "RESIDENCIA-MEDICA" -> "Residência Médica";
            default -> tipo;
        };
    }

    // -------------------------------------------------------------------------
    // FORMAÇÃO COMPLEMENTAR
    // -------------------------------------------------------------------------
    private void parseFormacaoComplementar(Curriculo cv, Element root) {
        NodeList secList = root.getElementsByTagName("FORMACAO-COMPLEMENTAR");
        if (secList.getLength() == 0) return;
        Element sec = (Element) secList.item(0);

        // Short courses
        NodeList cursos = sec.getElementsByTagName("FORMACAO-COMPLEMENTAR-CURSO-DE-CURTA-DURACAO");
        for (int i = 0; i < cursos.getLength(); i++) {
            Element el = (Element) cursos.item(i);
            FormacaoComplementar fc = new FormacaoComplementar();
            fc.setTipo("Curso de Curta Duração");
            fc.setNomeCurso(el.getAttribute("NOME-CURSO-CURTA-DURACAO"));
            fc.setNomeInstituicao(el.getAttribute("NOME-INSTITUICAO"));
            fc.setCargaHoraria(el.getAttribute("CARGA-HORARIA"));
            fc.setAnoInicio(el.getAttribute("ANO-DE-INICIO"));
            fc.setAnoConclusao(el.getAttribute("ANO-DE-CONCLUSAO"));
            fc.setStatusCurso(el.getAttribute("STATUS-DO-CURSO"));
            cv.getFormacoesComplementares().add(fc);
        }

        // University extension
        NodeList extensao = sec.getElementsByTagName("FORMACAO-COMPLEMENTAR-DE-EXTENSAO-UNIVERSITARIA");
        for (int i = 0; i < extensao.getLength(); i++) {
            Element el = (Element) extensao.item(i);
            FormacaoComplementar fc = new FormacaoComplementar();
            fc.setTipo("Extensão Universitária");
            fc.setNomeCurso(el.getAttribute("NOME-CURSO-EXTENSAO-UNIVERSITARIA"));
            fc.setNomeInstituicao(el.getAttribute("NOME-INSTITUICAO"));
            fc.setCargaHoraria(el.getAttribute("CARGA-HORARIA"));
            fc.setAnoInicio(el.getAttribute("ANO-DE-INICIO"));
            fc.setAnoConclusao(el.getAttribute("ANO-DE-CONCLUSAO"));
            fc.setStatusCurso(el.getAttribute("STATUS-DO-CURSO"));
            cv.getFormacoesComplementares().add(fc);
        }
    }

    // -------------------------------------------------------------------------
    // ATUAÇÕES PROFISSIONAIS
    // -------------------------------------------------------------------------
    private void parseAtuacoesProfissionais(Curriculo cv, Element root) {
        NodeList apList = root.getElementsByTagName("ATUACAO-PROFISSIONAL");
        for (int i = 0; i < apList.getLength(); i++) {
            Element ap = (Element) apList.item(i);
            String nomeInstituicao = ap.getAttribute("NOME-INSTITUICAO");

            NodeList vinculos = ap.getElementsByTagName("VINCULOS");
            if (vinculos.getLength() == 0) {
                // No bond detail — add institution-level entry
                AtuacaoProfissional a = new AtuacaoProfissional();
                a.setNomeInstituicao(nomeInstituicao);
                cv.getAtuacoes().add(a);
                continue;
            }

            for (int j = 0; j < vinculos.getLength(); j++) {
                Element v = (Element) vinculos.item(j);
                AtuacaoProfissional a = new AtuacaoProfissional();
                a.setNomeInstituicao(nomeInstituicao);
                a.setTipoVinculo(firstNonBlank(
                        v.getAttribute("TIPO-DE-VINCULO"),
                        v.getAttribute("OUTRO-VINCULO-INFORMADO")));
                a.setDescricao(firstNonBlank(
                        v.getAttribute("OUTRO-ENQUADRAMENTO-FUNCIONAL-INFORMADO"),
                        v.getAttribute("OUTRAS-INFORMACOES")));
                a.setAnoInicio(v.getAttribute("ANO-INICIO"));
                a.setMesInicio(v.getAttribute("MES-INICIO"));
                a.setAnoFim(v.getAttribute("ANO-FIM"));
                a.setMesFim(v.getAttribute("MES-FIM"));
                cv.getAtuacoes().add(a);
            }
        }
    }

    // -------------------------------------------------------------------------
    // PRODUÇÃO BIBLIOGRÁFICA
    // -------------------------------------------------------------------------
    private void parseProducaoBibliografica(Curriculo cv, Element root) {
        NodeList pbList = root.getElementsByTagName("PRODUCAO-BIBLIOGRAFICA");
        if (pbList.getLength() == 0) return;
        Element pb = (Element) pbList.item(0);

        // Articles
        NodeList artigos = pb.getElementsByTagName("ARTIGO-PUBLICADO");
        for (int i = 0; i < artigos.getLength(); i++) {
            cv.getArtigosPublicados().add(parseArtigo((Element) artigos.item(i)));
        }

        // Books
        NodeList livros = pb.getElementsByTagName("LIVRO-PUBLICADO-OU-ORGANIZADO");
        for (int i = 0; i < livros.getLength(); i++) {
            cv.getLivros().add(parseLivro((Element) livros.item(i)));
        }

        // Book chapters
        NodeList caps = pb.getElementsByTagName("CAPITULO-DE-LIVRO-PUBLICADO");
        for (int i = 0; i < caps.getLength(); i++) {
            cv.getCapitulosLivro().add(parseCapitulo((Element) caps.item(i)));
        }
    }

    private Publicacao parseArtigo(Element el) {
        Publicacao p = new Publicacao();
        p.setTipo("ARTIGO");
        NodeList dados = el.getElementsByTagName("DADOS-BASICOS-DO-ARTIGO");
        if (dados.getLength() > 0) {
            Element d = (Element) dados.item(0);
            p.setTitulo(d.getAttribute("TITULO-DO-ARTIGO"));
            p.setAno(d.getAttribute("ANO-DO-ARTIGO"));
            p.setDoi(d.getAttribute("DOI"));
        }
        NodeList det = el.getElementsByTagName("DETALHAMENTO-DO-ARTIGO");
        if (det.getLength() > 0) {
            Element d = (Element) det.item(0);
            p.setVeiculo(d.getAttribute("TITULO-DO-PERIODICO-OU-REVISTA"));
            p.setVolume(d.getAttribute("VOLUME"));
            p.setNumero(d.getAttribute("FASCICULO"));
            p.setPaginas(d.getAttribute("PAGINA-INICIAL") +
                    (d.getAttribute("PAGINA-FINAL").isBlank() ? "" : "-" + d.getAttribute("PAGINA-FINAL")));
        }
        p.setAutores(extractAuthors(el, "AUTORES", "NOME-COMPLETO-DO-AUTOR"));
        return p;
    }

    private Publicacao parseLivro(Element el) {
        Publicacao p = new Publicacao();
        p.setTipo("LIVRO");
        NodeList dados = el.getElementsByTagName("DADOS-BASICOS-DO-LIVRO");
        if (dados.getLength() > 0) {
            Element d = (Element) dados.item(0);
            p.setTitulo(d.getAttribute("TITULO-DO-LIVRO"));
            p.setAno(d.getAttribute("ANO"));
            p.setDoi(d.getAttribute("DOI"));
        }
        NodeList det = el.getElementsByTagName("DETALHAMENTO-DO-LIVRO");
        if (det.getLength() > 0) {
            Element d = (Element) det.item(0);
            p.setEditora(d.getAttribute("NOME-DA-EDITORA"));
            p.setCidade(d.getAttribute("CIDADE-DA-EDITORA"));
        }
        p.setAutores(extractAuthors(el, "AUTORES", "NOME-COMPLETO-DO-AUTOR"));
        return p;
    }

    private Publicacao parseCapitulo(Element el) {
        Publicacao p = new Publicacao();
        p.setTipo("CAPITULO");
        NodeList dados = el.getElementsByTagName("DADOS-BASICOS-DO-CAPITULO");
        if (dados.getLength() > 0) {
            Element d = (Element) dados.item(0);
            p.setTitulo(d.getAttribute("TITULO-DO-CAPITULO-DO-LIVRO"));
            p.setAno(d.getAttribute("ANO"));
            p.setDoi(d.getAttribute("DOI"));
        }
        NodeList det = el.getElementsByTagName("DETALHAMENTO-DO-CAPITULO");
        if (det.getLength() > 0) {
            Element d = (Element) det.item(0);
            p.setVeiculo(d.getAttribute("TITULO-DO-LIVRO"));
            p.setEditora(d.getAttribute("NOME-DA-EDITORA"));
            p.setPaginas(d.getAttribute("PAGINA-INICIAL") +
                    (d.getAttribute("PAGINA-FINAL").isBlank() ? "" : "-" + d.getAttribute("PAGINA-FINAL")));
        }
        p.setAutores(extractAuthors(el, "AUTORES", "NOME-COMPLETO-DO-AUTOR"));
        return p;
    }

    private String extractAuthors(Element parent, String listTag, String nameAttr) {
        NodeList autoresList = parent.getElementsByTagName(listTag);
        if (autoresList.getLength() == 0) return "";
        List<String> names = new ArrayList<>();
        for (int i = 0; i < autoresList.getLength(); i++) {
            Element a = (Element) autoresList.item(i);
            String name = a.getAttribute(nameAttr);
            if (name != null && !name.isBlank()) {
                names.add(name);
            }
        }
        return String.join("; ", names);
    }

    // -------------------------------------------------------------------------
    // PROJETOS DE PESQUISA
    // -------------------------------------------------------------------------
    private void parseProjetosPesquisa(Curriculo cv, Element root) {
        NodeList pdList = root.getElementsByTagName("PROJETO-DE-PESQUISA");
        for (int i = 0; i < pdList.getLength(); i++) {
            Element el = (Element) pdList.item(i);
            ProjetoPesquisa p = new ProjetoPesquisa();
            p.setTitulo(el.getAttribute("NOME-DO-PROJETO"));
            p.setDescricao(el.getAttribute("DESCRICAO-DO-PROJETO"));
            p.setAnoInicio(el.getAttribute("ANO-INICIO"));
            p.setAnoFim(el.getAttribute("ANO-FIM"));
            p.setSituacao(el.getAttribute("SITUACAO"));

            NodeList membros = el.getElementsByTagName("INTEGRANTES-DO-PROJETO");
            NodeList linhas = el.getElementsByTagName("LINHA-DE-PESQUISA");
            for (int j = 0; j < linhas.getLength(); j++) {
                String titulo = ((Element) linhas.item(j)).getAttribute("TITULO-DA-LINHA-DE-PESQUISA");
                if (titulo != null && !titulo.isBlank()) {
                    p.getLinhasPesquisa().add(titulo);
                }
            }
            cv.getProjetos().add(p);
        }
    }

    // -------------------------------------------------------------------------
    // ORIENTAÇÕES
    // -------------------------------------------------------------------------
    private void parseOrientacoes(Curriculo cv, Element root) {
        NodeList secList = root.getElementsByTagName("ORIENTACOES-CONCLUIDAS");
        List<Element> secoes = new ArrayList<>();
        for (int i = 0; i < secList.getLength(); i++) secoes.add((Element) secList.item(i));

        NodeList secAndamento = root.getElementsByTagName("ORIENTACOES-EM-ANDAMENTO");
        for (int i = 0; i < secAndamento.getLength(); i++) secoes.add((Element) secAndamento.item(i));

        for (Element sec : secoes) {
            String[][] tiposOrientacoes = {
                {"ORIENTACOES-CONCLUIDAS-PARA-DOUTORADO", "Doutorado"},
                {"ORIENTACOES-CONCLUIDAS-PARA-MESTRADO", "Mestrado"},
                {"MESTRADO-CONCLUIDO", "Mestrado"},
                {"DOUTORADO-CONCLUIDO", "Doutorado"},
                {"TRABALHO-DE-CONCLUSAO-DE-CURSO-GRADUACAO", "TCC"},
                {"INICIACAO-CIENTIFICA", "Iniciação Científica"},
                {"ORIENTACOES-EM-ANDAMENTO-PARA-DOUTORADO", "Doutorado (em andamento)"},
                {"ORIENTACOES-EM-ANDAMENTO-PARA-MESTRADO", "Mestrado (em andamento)"},
                {"OUTRA-ORIENTACAO-CONCLUIDA", "Outra orientação"},
                {"OUTRA-ORIENTACAO-EM-ANDAMENTO", "Outra orientação (em andamento)"}
            };

            for (String[] tipoInfo : tiposOrientacoes) {
                NodeList nodes = sec.getElementsByTagName(tipoInfo[0]);
                for (int i = 0; i < nodes.getLength(); i++) {
                    Element el = (Element) nodes.item(i);
                    Orientacao o = new Orientacao();
                    o.setTipo(tipoInfo[1]);

                    // Dados básicos
                    String[] basicTags = {
                        "DADOS-BASICOS-DE-ORIENTACOES-CONCLUIDAS-PARA-DOUTORADO",
                        "DADOS-BASICOS-DE-ORIENTACOES-CONCLUIDAS-PARA-MESTRADO",
                        "DADOS-BASICOS-DO-MESTRADO-CONCLUIDO",
                        "DADOS-BASICOS-DO-DOUTORADO-CONCLUIDO",
                        "DADOS-BASICOS-DO-TRABALHO-DE-CONCLUSAO-DE-CURSO-GRADUACAO",
                        "DADOS-BASICOS-DA-INICIACAO-CIENTIFICA",
                        "DADOS-BASICOS-DE-ORIENTACOES-EM-ANDAMENTO-PARA-DOUTORADO",
                        "DADOS-BASICOS-DE-ORIENTACOES-EM-ANDAMENTO-PARA-MESTRADO",
                        "DADOS-BASICOS-DE-OUTRAS-ORIENTACOES-CONCLUIDAS",
                        "DADOS-BASICOS-DE-OUTRAS-ORIENTACOES-EM-ANDAMENTO"
                    };
                    for (String basicTag : basicTags) {
                        NodeList basic = el.getElementsByTagName(basicTag);
                        if (basic.getLength() > 0) {
                            Element bd = (Element) basic.item(0);
                            o.setTitulo(bd.getAttribute("TITULO"));
                            o.setAnoConclusao(bd.getAttribute("ANO"));
                            break;
                        }
                    }

                    // Detalhamento
                    NodeList detList = el.getChildNodes();
                    for (int k = 0; k < detList.getLength(); k++) {
                        if (detList.item(k) instanceof Element det &&
                                det.getTagName().startsWith("DETALHAMENTO")) {
                            o.setNomeOrientando(firstNonBlank(
                                    det.getAttribute("NOME-DO-ORIENTANDO"),
                                    det.getAttribute("NOME-DO-ALUNO")));
                            o.setNomeInstituicao(det.getAttribute("NOME-DA-INSTITUICAO"));
                            o.setAnoInicio(det.getAttribute("ANO-DE-INICIO"));
                            break;
                        }
                    }

                    if (o.getTitulo() != null && !o.getTitulo().isBlank()) {
                        cv.getOrientacoes().add(o);
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // PRÊMIOS
    // -------------------------------------------------------------------------
    private void parsePremios(Curriculo cv, Element root) {
        NodeList ptList = root.getElementsByTagName("PREMIO-TITULO");
        for (int i = 0; i < ptList.getLength(); i++) {
            Element el = (Element) ptList.item(i);
            Premio p = new Premio();
            p.setNome(el.getAttribute("NOME-DO-PREMIO-OU-TITULO"));
            p.setAno(el.getAttribute("ANO-DA-PREMIACAO"));
            p.setEntidade(el.getAttribute("NOME-DA-ENTIDADE-PROMOTORA"));
            cv.getPremios().add(p);
        }
    }

    // -------------------------------------------------------------------------
    // IDIOMAS
    // -------------------------------------------------------------------------
    private void parseIdiomas(Curriculo cv, Element root) {
        NodeList idList = root.getElementsByTagName("IDIOMA");
        for (int i = 0; i < idList.getLength(); i++) {
            Element el = (Element) idList.item(i);
            Idioma id = new Idioma();
            id.setNome(el.getAttribute("IDIOMA"));
            id.setProficienciaConversacao(el.getAttribute("PROFICIENCIA-DE-CONVERSACAO"));
            id.setProficienciaLeitura(el.getAttribute("PROFICIENCIA-DE-LEITURA"));
            id.setProficienciaEscrita(el.getAttribute("PROFICIENCIA-DE-ESCRITA"));
            id.setProficienciaCompreensao(el.getAttribute("PROFICIENCIA-DE-COMPREENSAO-AUDITIVA"));
            cv.getIdiomas().add(id);
        }
    }

    // -------------------------------------------------------------------------
    // ÁREAS DE ATUAÇÃO
    // -------------------------------------------------------------------------
    private void parseAreasAtuacao(Curriculo cv, Element root) {
        NodeList aaList = root.getElementsByTagName("AREA-DE-ATUACAO");
        for (int i = 0; i < aaList.getLength(); i++) {
            Element el = (Element) aaList.item(i);
            AreaAtuacao aa = new AreaAtuacao();
            aa.setGrandeArea(firstNonBlank(
                    el.getAttribute("NOME-GRANDE-AREA-DO-CONHECIMENTO"),
                    el.getAttribute("GRANDE-AREA")));
            aa.setArea(firstNonBlank(
                    el.getAttribute("NOME-DA-AREA-DO-CONHECIMENTO"),
                    el.getAttribute("AREA")));
            aa.setSubArea(firstNonBlank(
                    el.getAttribute("NOME-DA-SUB-AREA-DO-CONHECIMENTO"),
                    el.getAttribute("SUB-AREA")));
            aa.setEspecialidade(firstNonBlank(
                    el.getAttribute("NOME-DA-ESPECIALIDADE"),
                    el.getAttribute("ESPECIALIDADE")));
            cv.getAreasAtuacao().add(aa);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------
    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }
}
