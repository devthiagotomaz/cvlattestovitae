package br.com.cvlattestovitae.service;

import br.com.cvlattestovitae.model.*;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * Generates a Word (.docx) document from a {@link Curriculo}.
 * Uses Apache POI XWPF.
 */
@Service
public class WordGeneratorService {

    // Colours (hex without #)
    private static final String COLOR_PRIMARY   = "1A3C5E";
    private static final String COLOR_SECONDARY = "2B5A8A";
    private static final String COLOR_GRAY      = "555555";
    private static final String COLOR_LIGHT     = "666666";

    public byte[] generateWord(Curriculo cv) throws Exception {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            setPageMargins(doc);

            // ---- Header: Name ----
            addName(doc, cv.getNomeCompleto());
            addContactLine(doc, cv);
            addHorizontalRule(doc);

            // ---- Resumo ----
            if (notBlank(cv.getResumo())) {
                addSectionTitle(doc, "RESUMO");
                addBodyText(doc, cv.getResumo());
            }

            // ---- Áreas de atuação ----
            if (notEmpty(cv.getAreasAtuacao())) {
                addSectionTitle(doc, "ÁREAS DE ATUAÇÃO");
                for (AreaAtuacao a : cv.getAreasAtuacao()) {
                    String text = joinNonBlank(" › ", a.getGrandeArea(), a.getArea(),
                            a.getSubArea(), a.getEspecialidade());
                    addBullet(doc, text);
                }
            }

            // ---- Formação acadêmica ----
            if (notEmpty(cv.getFormacoes())) {
                addSectionTitle(doc, "FORMAÇÃO ACADÊMICA");
                for (FormacaoAcademica f : cv.getFormacoes()) {
                    addEntryTitle(doc, f.getNomeCurso() + " — " + f.getTipo());
                    addEntrySubtitle(doc, f.getNomeInstituicao());
                    String period = f.getAnoInicio() + " – " +
                            (notBlank(f.getAnoConclusao()) ? f.getAnoConclusao() : "Atual") +
                            ("EM_ANDAMENTO".equals(f.getStatusCurso()) ? " (Em andamento)" : "");
                    addMeta(doc, period);
                    if (notBlank(f.getTituloDissertacao()))
                        addMeta(doc, "Título: " + f.getTituloDissertacao());
                    if (notBlank(f.getNomeOrientador()))
                        addMeta(doc, "Orientador(a): " + f.getNomeOrientador());
                    addSpacing(doc);
                }
            }

            // ---- Formação complementar ----
            if (notEmpty(cv.getFormacoesComplementares())) {
                addSectionTitle(doc, "FORMAÇÃO COMPLEMENTAR");
                for (FormacaoComplementar fc : cv.getFormacoesComplementares()) {
                    addEntryTitle(doc, fc.getNomeCurso() + " (" + fc.getTipo() + ")");
                    addEntrySubtitle(doc, fc.getNomeInstituicao());
                    String period = notBlank(fc.getAnoInicio()) ? fc.getAnoInicio() : "";
                    if (notBlank(fc.getAnoConclusao())) period += " – " + fc.getAnoConclusao();
                    if (notBlank(fc.getCargaHoraria())) period += " | " + fc.getCargaHoraria() + "h";
                    if (notBlank(period)) addMeta(doc, period);
                    addSpacing(doc);
                }
            }

            // ---- Experiência profissional ----
            if (notEmpty(cv.getAtuacoes())) {
                addSectionTitle(doc, "EXPERIÊNCIA PROFISSIONAL");
                for (AtuacaoProfissional a : cv.getAtuacoes()) {
                    addEntryTitle(doc, a.getNomeInstituicao());
                    if (notBlank(a.getTipoVinculo()))
                        addEntrySubtitle(doc, a.getTipoVinculo());
                    if (notBlank(a.getAnoInicio())) {
                        String period = monthYear(a.getMesInicio(), a.getAnoInicio()) + " – " +
                                (notBlank(a.getAnoFim())
                                        ? monthYear(a.getMesFim(), a.getAnoFim())
                                        : "Atual");
                        addMeta(doc, period);
                    }
                    if (notBlank(a.getDescricao()))
                        addBodyText(doc, a.getDescricao());
                    addSpacing(doc);
                }
            }

            // ---- Artigos publicados ----
            if (notEmpty(cv.getArtigosPublicados())) {
                addSectionTitle(doc, "ARTIGOS PUBLICADOS");
                int n = 1;
                for (Publicacao p : cv.getArtigosPublicados()) {
                    addNumberedPublication(doc, n++, p.getTitulo(), p.getAutores(),
                            buildArtigoRef(p), p.getDoi());
                }
            }

            // ---- Livros ----
            if (notEmpty(cv.getLivros())) {
                addSectionTitle(doc, "LIVROS PUBLICADOS / ORGANIZADOS");
                int n = 1;
                for (Publicacao p : cv.getLivros()) {
                    addNumberedPublication(doc, n++, p.getTitulo(), p.getAutores(),
                            buildLivroRef(p), p.getDoi());
                }
            }

            // ---- Capítulos ----
            if (notEmpty(cv.getCapitulosLivro())) {
                addSectionTitle(doc, "CAPÍTULOS DE LIVROS");
                int n = 1;
                for (Publicacao p : cv.getCapitulosLivro()) {
                    addNumberedPublication(doc, n++, p.getTitulo(), p.getAutores(),
                            buildCapituloRef(p), p.getDoi());
                }
            }

            // ---- Projetos de pesquisa ----
            if (notEmpty(cv.getProjetos())) {
                addSectionTitle(doc, "PROJETOS DE PESQUISA");
                for (ProjetoPesquisa p : cv.getProjetos()) {
                    addEntryTitle(doc, p.getTitulo());
                    if (notBlank(p.getNomeOrgao()))
                        addEntrySubtitle(doc, p.getNomeOrgao());
                    String period = notBlank(p.getAnoInicio())
                            ? p.getAnoInicio() + " – " + (notBlank(p.getAnoFim()) ? p.getAnoFim() : "Atual")
                            : "";
                    if (notBlank(p.getSituacao()))
                        period += (period.isBlank() ? "" : " | ") + p.getSituacao();
                    if (notBlank(period)) addMeta(doc, period);
                    if (notBlank(p.getDescricao())) addBodyText(doc, p.getDescricao());
                    if (notEmpty(p.getLinhasPesquisa()))
                        addMeta(doc, "Linhas: " + String.join("; ", p.getLinhasPesquisa()));
                    addSpacing(doc);
                }
            }

            // ---- Orientações ----
            if (notEmpty(cv.getOrientacoes())) {
                addSectionTitle(doc, "ORIENTAÇÕES");
                for (Orientacao o : cv.getOrientacoes()) {
                    addEntryTitle(doc, o.getTitulo());
                    XWPFParagraph sub = doc.createParagraph();
                    sub.setSpacingAfter(0);
                    XWPFRun r1 = sub.createRun();
                    r1.setFontSize(9);
                    r1.setColor(COLOR_SECONDARY);
                    r1.setBold(true);
                    r1.setText("[" + o.getTipo() + "]");
                    if (notBlank(o.getNomeOrientando())) {
                        r1.setText("[" + o.getTipo() + "]  Orientando(a): " + o.getNomeOrientando());
                    }
                    if (notBlank(o.getNomeInstituicao())) {
                        String ref = o.getNomeInstituicao() +
                                (notBlank(o.getAnoConclusao()) ? " (" + o.getAnoConclusao() + ")" : "");
                        addMeta(doc, ref);
                    }
                    addSpacing(doc);
                }
            }

            // ---- Prêmios ----
            if (notEmpty(cv.getPremios())) {
                addSectionTitle(doc, "PRÊMIOS E DISTINÇÕES");
                for (Premio p : cv.getPremios()) {
                    addEntryTitle(doc, p.getNome());
                    if (notBlank(p.getEntidade())) addEntrySubtitle(doc, p.getEntidade());
                    if (notBlank(p.getAno())) addMeta(doc, p.getAno());
                    addSpacing(doc);
                }
            }

            // ---- Idiomas ----
            if (notEmpty(cv.getIdiomas())) {
                addSectionTitle(doc, "IDIOMAS");
                for (Idioma id : cv.getIdiomas()) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setSpacingAfter(20);
                    XWPFRun rLabel = p.createRun();
                    rLabel.setBold(true);
                    rLabel.setColor(COLOR_PRIMARY);
                    rLabel.setFontSize(10);
                    rLabel.setText(id.getNome() + ": ");
                    XWPFRun rVal = p.createRun();
                    rVal.setFontSize(10);
                    rVal.setColor(COLOR_GRAY);
                    rVal.setText(buildIdiomaDetail(id));
                }
            }

            // ---- Footer line ----
            addHorizontalRule(doc);
            XWPFParagraph footer = doc.createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun fr = footer.createRun();
            fr.setFontSize(8);
            fr.setColor("999999");
            fr.setText("Currículo gerado a partir da Plataforma Lattes (CNPq)" +
                    (notBlank(cv.getLattesId()) ? " — ID Lattes: " + cv.getLattesId() : ""));

            doc.write(baos);
            return baos.toByteArray();
        }
    }

    // =========================================================================
    // Paragraph helpers
    // =========================================================================

    private void setPageMargins(XWPFDocument doc) {
        CTSectPr sectPr = doc.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        // 2 cm margins (1 cm = 567 twips)
        long margin = 1134; // ~2 cm
        pageMar.setTop(BigInteger.valueOf(margin));
        pageMar.setBottom(BigInteger.valueOf(margin));
        pageMar.setLeft(BigInteger.valueOf(margin));
        pageMar.setRight(BigInteger.valueOf(margin));
    }

    private void addName(XWPFDocument doc, String name) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        p.setSpacingAfter(40);
        XWPFRun r = p.createRun();
        r.setText(name != null ? name : "");
        r.setFontSize(22);
        r.setBold(true);
        r.setColor(COLOR_PRIMARY);
        r.setFontFamily("Calibri");
    }

    private void addContactLine(XWPFDocument doc, Curriculo cv) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(60);

        if (notBlank(cv.getLattesId())) addContactRun(p, "Lattes: " + cv.getLattesId());
        if (notBlank(cv.getOrcid()))    addContactRun(p, "  |  ORCID: " + cv.getOrcid());
        if (notBlank(cv.getEmail()))    addContactRun(p, "  |  " + cv.getEmail());
        if (notBlank(cv.getHomePage())) addContactRun(p, "  |  " + cv.getHomePage());

        if (notBlank(cv.getNomeInstituicaoEndereco())) {
            String inst = cv.getNomeInstituicaoEndereco();
            if (notBlank(cv.getCidadeEndereco()))
                inst += ", " + cv.getCidadeEndereco() + "/" + cv.getUfEndereco();
            XWPFParagraph p2 = doc.createParagraph();
            p2.setSpacingAfter(0);
            XWPFRun r = p2.createRun();
            r.setFontSize(9);
            r.setItalic(true);
            r.setColor(COLOR_GRAY);
            r.setText(inst);
        }
    }

    private void addContactRun(XWPFParagraph p, String text) {
        XWPFRun r = p.createRun();
        r.setFontSize(9);
        r.setColor(COLOR_GRAY);
        r.setText(text);
    }

    private void addHorizontalRule(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(0);
        p.setSpacingAfter(80);
        CTBorder border = CTBorder.Factory.newInstance();
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(6));
        border.setColor(COLOR_PRIMARY);
        CTPPr pPr = p.getCTP().addNewPPr();
        CTPBdr pBdr = pPr.addNewPBdr();
        pBdr.setBottom(border);
    }

    private void addSectionTitle(XWPFDocument doc, String title) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingBefore(160);
        p.setSpacingAfter(60);
        CTPPr pPr = p.getCTP().getPPr() != null ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTPBdr pBdr = pPr.isSetPBdr() ? pPr.getPBdr() : pPr.addNewPBdr();
        CTBorder border = CTBorder.Factory.newInstance();
        border.setVal(STBorder.SINGLE);
        border.setSz(BigInteger.valueOf(4));
        border.setColor(COLOR_PRIMARY);
        pBdr.setBottom(border);
        XWPFRun r = p.createRun();
        r.setText(title);
        r.setBold(true);
        r.setFontSize(11);
        r.setColor(COLOR_PRIMARY);
        r.setFontFamily("Calibri");
    }

    private void addEntryTitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setBold(true);
        r.setFontSize(10);
        r.setColor("222222");
    }

    private void addEntrySubtitle(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(10);
        r.setColor(COLOR_GRAY);
    }

    private void addMeta(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(9);
        r.setColor(COLOR_LIGHT);
    }

    private void addBodyText(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontSize(10);
        r.setColor(COLOR_GRAY);
        r.setItalic(true);
    }

    private void addBullet(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(20);
        CTPPr pPr = p.getCTP().getPPr() != null ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setLeft(BigInteger.valueOf(360));
        XWPFRun r = p.createRun();
        r.setText("• " + text);
        r.setFontSize(9);
        r.setColor(COLOR_GRAY);
    }

    private void addSpacing(XWPFDocument doc) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        p.setSpacingBefore(0);
        XWPFRun r = p.createRun();
        r.setFontSize(4);
        r.setText("");
    }

    private void addNumberedPublication(XWPFDocument doc, int num, String titulo,
                                        String autores, String ref, String doi) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(0);
        CTPPr pPr = p.getCTP().getPPr() != null ? p.getCTP().getPPr() : p.getCTP().addNewPPr();
        CTInd ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
        ind.setLeft(BigInteger.valueOf(360));
        ind.setHanging(BigInteger.valueOf(360));

        XWPFRun rNum = p.createRun();
        rNum.setText(num + ". ");
        rNum.setFontSize(10);
        rNum.setBold(true);
        rNum.setColor(COLOR_PRIMARY);

        XWPFRun rTitle = p.createRun();
        rTitle.setText(titulo != null ? titulo : "");
        rTitle.setFontSize(10);
        rTitle.setBold(true);
        rTitle.setColor("222222");

        if (notBlank(autores)) {
            XWPFParagraph pa = doc.createParagraph();
            pa.setSpacingAfter(0);
            CTInd paInd = pa.getCTP().addNewPPr().addNewInd();
            paInd.setLeft(BigInteger.valueOf(360));
            XWPFRun ra = pa.createRun();
            ra.setText(autores);
            ra.setFontSize(9);
            ra.setColor(COLOR_GRAY);
        }

        if (notBlank(ref)) {
            XWPFParagraph pr = doc.createParagraph();
            pr.setSpacingAfter(0);
            CTInd prInd = pr.getCTP().addNewPPr().addNewInd();
            prInd.setLeft(BigInteger.valueOf(360));
            XWPFRun rr = pr.createRun();
            rr.setText(ref);
            rr.setFontSize(9);
            rr.setColor(COLOR_LIGHT);
            rr.setItalic(true);
        }

        if (notBlank(doi)) {
            XWPFParagraph pd = doc.createParagraph();
            pd.setSpacingAfter(0);
            CTInd pdInd = pd.getCTP().addNewPPr().addNewInd();
            pdInd.setLeft(BigInteger.valueOf(360));
            XWPFRun rd = pd.createRun();
            rd.setText("DOI: " + doi);
            rd.setFontSize(8);
            rd.setColor("0057A8");
        }

        addSpacing(doc);
    }

    // =========================================================================
    // Reference string builders
    // =========================================================================

    private String buildArtigoRef(Publicacao p) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(p.getVeiculo())) sb.append(p.getVeiculo());
        if (notBlank(p.getVolume())) sb.append(", v.").append(p.getVolume());
        if (notBlank(p.getNumero())) sb.append(", n.").append(p.getNumero());
        if (notBlank(p.getPaginas())) sb.append(", p.").append(p.getPaginas());
        if (notBlank(p.getAno())) sb.append(" (").append(p.getAno()).append(")");
        return sb.toString();
    }

    private String buildLivroRef(Publicacao p) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(p.getEditora())) sb.append(p.getEditora());
        if (notBlank(p.getCidade())) sb.append(", ").append(p.getCidade());
        if (notBlank(p.getAno())) sb.append(" (").append(p.getAno()).append(")");
        return sb.toString();
    }

    private String buildCapituloRef(Publicacao p) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(p.getVeiculo())) sb.append("In: ").append(p.getVeiculo());
        if (notBlank(p.getEditora())) sb.append(". ").append(p.getEditora());
        if (notBlank(p.getPaginas())) sb.append(", p.").append(p.getPaginas());
        if (notBlank(p.getAno())) sb.append(" (").append(p.getAno()).append(")");
        return sb.toString();
    }

    private String buildIdiomaDetail(Idioma id) {
        StringBuilder sb = new StringBuilder();
        if (notBlank(id.getProficienciaLeitura()))     sb.append("Leitura: ").append(id.getProficienciaLeitura());
        if (notBlank(id.getProficienciaEscrita()))     append(sb, "Escrita: " + id.getProficienciaEscrita());
        if (notBlank(id.getProficienciaConversacao())) append(sb, "Fala: " + id.getProficienciaConversacao());
        if (notBlank(id.getProficienciaCompreensao())) append(sb, "Compreensão: " + id.getProficienciaCompreensao());
        return sb.toString();
    }

    // =========================================================================
    // Utilities
    // =========================================================================

    private void append(StringBuilder sb, String part) {
        if (sb.length() > 0) sb.append(" · ");
        sb.append(part);
    }

    private String joinNonBlank(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (notBlank(part)) {
                if (sb.length() > 0) sb.append(sep);
                sb.append(part);
            }
        }
        return sb.toString();
    }

    private String monthYear(String mes, String ano) {
        if (notBlank(mes)) return mes + "/" + ano;
        return ano != null ? ano : "";
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private boolean notEmpty(List<?> list) {
        return list != null && !list.isEmpty();
    }
}
