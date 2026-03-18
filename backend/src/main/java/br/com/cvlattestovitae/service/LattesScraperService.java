package br.com.cvlattestovitae.service;

import br.com.cvlattestovitae.exception.CaptchaRequiredException;
import br.com.cvlattestovitae.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Service
public class LattesScraperService {

    private static final String ALLOWED_HOST = "lattes.cnpq.br";
    private static final String ALLOWED_HOST_ALT = "buscatextual.cnpq.br";
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 30_000;

    /**
     * Validates that the supplied URL belongs to one of the allowed Lattes domains.
     *
     * @param url the URL to validate
     * @throws IllegalArgumentException when the URL is blank, malformed, or not from lattes.cnpq.br or buscatextual.cnpq.br
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL não informada.");
        }
        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (host == null
                    || (!host.toLowerCase().endsWith(ALLOWED_HOST)
                            && !host.toLowerCase().endsWith(ALLOWED_HOST_ALT))) {
                throw new IllegalArgumentException(
                        "URL inválida. Apenas URLs dos domínios lattes.cnpq.br ou buscatextual.cnpq.br são aceitas.");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL malformada: " + e.getMessage());
        }
    }

    /**
     * Fetches the public Lattes curriculum page and maps the HTML content to
     * a {@link Curriculo} object.
     *
     * @param url public Lattes profile URL (must be from lattes.cnpq.br or buscatextual.cnpq.br)
     * @return populated {@link Curriculo}
     * @throws IllegalArgumentException for invalid / disallowed URLs
     * @throws IOException              when the page cannot be fetched
     */
    public Curriculo scrape(String url) throws IOException {
        validateUrl(url);

        Document doc = Jsoup.connect(url.trim())
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

        if (isCaptchaPage(doc)) {
            throw new CaptchaRequiredException(url.trim());
        }

        Curriculo curriculo = new Curriculo();

        extractNome(doc, curriculo);
        extractResumo(doc, curriculo);
        extractEnderecoProfissional(doc, curriculo);
        extractAreasAtuacao(doc, curriculo);
        extractIdiomas(doc, curriculo);
        extractFormacao(doc, curriculo);
        extractAtuacaoProfissional(doc, curriculo);
        extractArtigos(doc, curriculo);
        extractLivros(doc, curriculo);
        extractCapitulos(doc, curriculo);
        extractTrabalhosCongresso(doc, curriculo);
        extractProjetos(doc, curriculo);
        extractOrientacoes(doc, curriculo);
        extractPremios(doc, curriculo);

        return curriculo;
    }

    // -------------------------------------------------------------------------
    // Captcha detection
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the fetched page is a CAPTCHA challenge page
     * (not the actual curriculum).
     */
    private boolean isCaptchaPage(Document doc) {
        // CNPq captcha page contains an input named "tokenCaptchar" and/or a
        // reCAPTCHA widget, and its title includes "Código de Segurança".
        boolean hasCaptchaInput = doc.selectFirst(
                "input[name=tokenCaptchar], .g-recaptcha, #g-recaptcha, "
                + "iframe[src*=recaptcha], form[action*=captchar]") != null;
        boolean hasCaptchaTitle = doc.title().toLowerCase().contains("código de segurança")
                || doc.title().toLowerCase().contains("codigo de seguranca")
                || doc.title().toLowerCase().contains("captcha");
        // If the page contains no typical CV elements, also treat it as captcha/error
        boolean hasNoCvContent = doc.selectFirst(
                "#pagina-curriculo, #curriculo, .dados-curriculo, "
                + ".layout-cell-pad-main, #content-cv") == null
                && doc.getElementsByTag("b").isEmpty();
        return hasCaptchaInput || hasCaptchaTitle || hasNoCvContent;
    }

    // -------------------------------------------------------------------------
    // Extraction helpers
    // -------------------------------------------------------------------------

    private void extractNome(Document doc, Curriculo curriculo) {
        // Primary selector used by buscatextual.cnpq.br/visualizacv.do
        Element nome = doc.selectFirst("h2.nome, div#nome h1, div#nome h2, div#dados-gerais h2");
        if (nome != null) {
            curriculo.setNomeCompleto(nome.text().trim());
            return;
        }
        // Fallback: generic Lattes public-profile selectors
        Element fallback = doc.selectFirst("h2.title, .nome, #nomeCompleto, .layout-cell-pad-main h2");
        if (fallback != null) {
            curriculo.setNomeCompleto(fallback.text().trim());
        }
    }

    private void extractResumo(Document doc, Curriculo curriculo) {
        // Selectors for buscatextual.cnpq.br/visualizacv.do
        Element resumoEl = doc.selectFirst("div#resumo, div.resumo, .resumo-cv, #mini-bio, [data-name='Resumo']");
        if (resumoEl != null) {
            curriculo.setResumo(resumoEl.text().trim());
            return;
        }
        // Fallback: look for a paragraph tagged "Resumo" inside layout cells
        for (Element cell : doc.select(".layout-cell-pad-main")) {
            String text = cell.text();
            if (text.length() > 50 && !text.contains("Formação") && !text.contains("Atuação")) {
                // The first sizeable paragraph is typically the résumé
                curriculo.setResumo(text.trim());
                break;
            }
        }
    }

    private void extractEnderecoProfissional(Document doc, Curriculo curriculo) {
        // The professional address block is often inside a section titled "Endereço Profissional"
        Element section = findSectionByTitle(doc, "Endereço Profissional");
        if (section != null) {
            String text = section.text().trim();
            curriculo.setNomeInstituicaoEndereco(text);
        }
    }

    private void extractAreasAtuacao(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Áreas de atuação");
        if (section == null) section = findSectionByTitle(doc, "Areas de atuacao");
        if (section == null) return;

        List<AreaAtuacao> areas = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, li, .dados-producao")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            AreaAtuacao area = new AreaAtuacao();
            // Try to split by known delimiters (" / " or " > ")
            String[] parts = text.split(" / | > ");
            area.setGrandeArea(parts.length > 0 ? parts[0].trim() : text);
            if (parts.length > 1) area.setArea(parts[1].trim());
            if (parts.length > 2) area.setSubArea(parts[2].trim());
            if (parts.length > 3) area.setEspecialidade(parts[3].trim());
            areas.add(area);
        }
        // If no structured items found, use raw text lines
        if (areas.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.toLowerCase().contains("área")) {
                    AreaAtuacao area = new AreaAtuacao();
                    area.setGrandeArea(trimmed);
                    areas.add(area);
                }
            }
        }
        curriculo.setAreasAtuacao(areas);
    }

    private void extractIdiomas(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Idiomas");
        if (section == null) return;

        List<Idioma> idiomas = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, li, .dados-producao")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            Idioma idioma = new Idioma();
            idioma.setNome(text);
            idiomas.add(idioma);
        }
        if (idiomas.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.equalsIgnoreCase("Idiomas")) {
                    Idioma idioma = new Idioma();
                    idioma.setNome(trimmed);
                    idiomas.add(idioma);
                }
            }
        }
        curriculo.setIdiomas(idiomas);
    }

    private void extractFormacao(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Formação acadêmica");
        if (section == null) section = findSectionByTitle(doc, "Formacao academica");
        if (section == null) return;

        List<FormacaoAcademica> formacoes = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, .dados-producao, li")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            FormacaoAcademica f = parseFormacao(text);
            formacoes.add(f);
        }
        if (formacoes.isEmpty()) {
            // Parse raw text: each entry typically on a separate line
            for (String line : section.text().split("\\n|(?=Doutorado|Mestrado|Graduação|Especializ|Bacharel|Licenciatura)")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                FormacaoAcademica f = parseFormacao(trimmed);
                formacoes.add(f);
            }
        }
        curriculo.setFormacoes(formacoes);
    }

    private FormacaoAcademica parseFormacao(String text) {
        FormacaoAcademica f = new FormacaoAcademica();
        // Detect type from leading keyword
        if (text.toLowerCase().startsWith("doutorado")) f.setTipo("Doutorado");
        else if (text.toLowerCase().startsWith("mestrado")) f.setTipo("Mestrado");
        else if (text.toLowerCase().startsWith("graduação") || text.toLowerCase().startsWith("graduacao"))
            f.setTipo("Graduação");
        else if (text.toLowerCase().startsWith("especializ")) f.setTipo("Especialização");
        else if (text.toLowerCase().startsWith("bacharel")) f.setTipo("Bacharelado");
        else if (text.toLowerCase().startsWith("licenciatura")) f.setTipo("Licenciatura");
        else f.setTipo("Formação");

        // Try to extract year range like "(YYYY - YYYY)" or "(YYYY)"
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\(?(\\d{4})\\s*[-–]\\s*(\\d{4}|Em andamento|Atual)\\)?")
                .matcher(text);
        if (m.find()) {
            f.setAnoInicio(m.group(1));
            f.setAnoConclusao(m.group(2));
        }
        f.setNomeCurso(text);
        return f;
    }

    private void extractAtuacaoProfissional(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Atuação Profissional");
        if (section == null) section = findSectionByTitle(doc, "Atuacao Profissional");
        if (section == null) section = findSectionByTitle(doc, "Vínculos empregatícios");
        if (section == null) return;

        List<AtuacaoProfissional> atuacoes = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, .dados-producao, li")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            AtuacaoProfissional a = new AtuacaoProfissional();
            a.setNomeInstituicao(text);
            // Try to extract year range
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d{4})\\s*[-–]\\s*(\\d{4}|Em andamento|Atual|Presente)")
                    .matcher(text);
            if (m.find()) {
                a.setAnoInicio(m.group(1));
                a.setAnoFim(m.group(2));
            }
            atuacoes.add(a);
        }
        if (atuacoes.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.toLowerCase().contains("atuação")) continue;
                AtuacaoProfissional a = new AtuacaoProfissional();
                a.setNomeInstituicao(trimmed);
                atuacoes.add(a);
            }
        }
        curriculo.setAtuacoes(atuacoes);
    }

    private void extractArtigos(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Artigos completos publicados em periódicos");
        if (section == null) section = findSectionByTitle(doc, "Artigos publicados");
        if (section == null) return;

        curriculo.setArtigosPublicados(extractPublicacoes(section, "ARTIGO"));
    }

    private void extractLivros(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Livros publicados");
        if (section == null) section = findSectionByTitle(doc, "Livros organizados");
        if (section == null) return;

        curriculo.setLivros(extractPublicacoes(section, "LIVRO"));
    }

    private void extractCapitulos(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Capítulos de livros publicados");
        if (section == null) section = findSectionByTitle(doc, "Capitulos de livros");
        if (section == null) return;

        curriculo.setCapitulosLivro(extractPublicacoes(section, "CAPITULO"));
    }

    private void extractTrabalhosCongresso(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Trabalhos publicados em anais de congressos");
        if (section == null) section = findSectionByTitle(doc, "Trabalhos em anais");
        if (section == null) return;

        // Store congress papers as ARTIGO type with a distinguishing prefix
        List<Publicacao> congressos = extractPublicacoes(section, "CONGRESSO");
        // Merge into artigos list
        curriculo.getArtigosPublicados().addAll(congressos);
    }

    private List<Publicacao> extractPublicacoes(Element section, String tipo) {
        List<Publicacao> publicacoes = new ArrayList<>();
        // Prefer structured containers; avoid mixing parent and child selectors to prevent duplicates.
        // .cita-artigo is used by buscatextual.cnpq.br/visualizacv.do; .layout-cell-02 holds the
        // human-readable citation text inside each .cita-artigo.
        Elements citaItems = section.select(".cita-artigo");
        if (!citaItems.isEmpty()) {
            for (Element item : citaItems) {
                // Prefer the content column (.layout-cell-02) for cleaner text; fall back to full item text
                Element contentEl = item.selectFirst(".layout-cell-02, .layout-cell-pad-02");
                String text = (contentEl != null ? contentEl : item).text().trim();
                if (text.isEmpty()) continue;
                Publicacao p = new Publicacao();
                p.setTipo(tipo);
                p.setTitulo(text);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(text);
                if (m.find()) p.setAno(m.group(1));
                publicacoes.add(p);
            }
            return publicacoes;
        }
        // Fall back to structured production entries used by other Lattes page variants
        Elements items = section.select(".informacoes-producao, .dados-producao");
        if (!items.isEmpty()) {
            for (Element item : items) {
                String text = item.text().trim();
                if (text.isEmpty()) continue;
                Publicacao p = new Publicacao();
                p.setTipo(tipo);
                p.setTitulo(text);
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(text);
                if (m.find()) p.setAno(m.group(1));
                publicacoes.add(p);
            }
            return publicacoes;
        }
        // Last resort: split raw text of the section by newlines
        for (String line : section.text().split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            Publicacao p = new Publicacao();
            p.setTipo(tipo);
            p.setTitulo(trimmed);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(trimmed);
            if (m.find()) p.setAno(m.group(1));
            publicacoes.add(p);
        }
        return publicacoes;
    }

    private void extractProjetos(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Projetos de pesquisa");
        if (section == null) return;

        List<ProjetoPesquisa> projetos = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, .dados-producao, li")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            ProjetoPesquisa p = new ProjetoPesquisa();
            p.setTitulo(text);
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("(\\d{4})\\s*[-–]\\s*(\\d{4}|Em andamento|Atual)")
                    .matcher(text);
            if (m.find()) {
                p.setAnoInicio(m.group(1));
                p.setAnoFim(m.group(2));
            }
            projetos.add(p);
        }
        if (projetos.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.toLowerCase().contains("projeto")) continue;
                ProjetoPesquisa p = new ProjetoPesquisa();
                p.setTitulo(trimmed);
                projetos.add(p);
            }
        }
        curriculo.setProjetos(projetos);
    }

    private void extractOrientacoes(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Orientações");
        if (section == null) section = findSectionByTitle(doc, "Orientacoes");
        if (section == null) return;

        List<Orientacao> orientacoes = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, .dados-producao, li")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            Orientacao o = new Orientacao();
            o.setTitulo(text);
            if (text.toLowerCase().contains("doutorado")) o.setTipo("DOUTORADO");
            else if (text.toLowerCase().contains("mestrado")) o.setTipo("MESTRADO");
            else if (text.toLowerCase().contains("iniciação") || text.toLowerCase().contains("iniciacao"))
                o.setTipo("INICIACAO_CIENTIFICA");
            else o.setTipo("ORIENTACAO");
            if (text.toLowerCase().contains("em andamento") || text.toLowerCase().contains("andamento"))
                o.setSituacao("EM_ANDAMENTO");
            else o.setSituacao("CONCLUIDA");
            orientacoes.add(o);
        }
        if (orientacoes.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.toLowerCase().contains("orientaç")) continue;
                Orientacao o = new Orientacao();
                o.setTitulo(trimmed);
                o.setTipo("ORIENTACAO");
                orientacoes.add(o);
            }
        }
        curriculo.setOrientacoes(orientacoes);
    }

    private void extractPremios(Document doc, Curriculo curriculo) {
        Element section = findSectionByTitle(doc, "Prêmios e títulos");
        if (section == null) section = findSectionByTitle(doc, "Premios e titulos");
        if (section == null) section = findSectionByTitle(doc, "Prêmios");
        if (section == null) return;

        List<Premio> premios = new ArrayList<>();
        for (Element item : section.select(".informacoes-producao, .dados-producao, li")) {
            String text = item.text().trim();
            if (text.isEmpty()) continue;
            Premio p = new Premio();
            p.setNome(text);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})").matcher(text);
            if (m.find()) p.setAno(m.group(1));
            premios.add(p);
        }
        if (premios.isEmpty()) {
            for (String line : section.text().split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.toLowerCase().contains("prêmio")) continue;
                Premio p = new Premio();
                p.setNome(trimmed);
                premios.add(p);
            }
        }
        curriculo.setPremios(premios);
    }

    // -------------------------------------------------------------------------
    // Generic section finder
    // -------------------------------------------------------------------------

    /**
     * Finds the nearest content block for a Lattes HTML section header.
     * <p>
     * Handles two page structures:
     * <ul>
     *   <li><b>buscatextual.cnpq.br/visualizacv.do</b> — section titles are in {@code <b>} tags
     *       inside the same {@code <div>} that contains the {@code <ol>} items.</li>
     *   <li><b>lattes.cnpq.br</b> public-profile pages — section titles are in
     *       {@code <div class="title">} / {@code <span class="title">} elements whose next sibling
     *       is the content container.</li>
     * </ul>
     */
    private Element findSectionByTitle(Document doc, String sectionTitle) {
        // Strategy 1: look for any element whose text exactly/partially matches the title.
        // h1 is added here because buscatextual.cnpq.br/visualizacv.do wraps each section
        // title in an <h1> tag inside a div that also holds the content items.
        // <b> handles the older visualizacv.do layout which uses <b>Section Name</b>.
        for (Element el : doc.select("h1, span.title, div.title, b, h2, h3, h4, dt, .group-title")) {
            if (el.text().trim().equalsIgnoreCase(sectionTitle)
                    || el.text().trim().toLowerCase().contains(sectionTitle.toLowerCase())) {
                Element parent = el.parent();
                if (parent != null) {
                    // If the parent div directly contains content items the header element belongs
                    // to the same container as the data (visualizacv.do layout); return the parent.
                    if (!parent.select("li, .informacoes-producao, .dados-producao, .cita-artigo").isEmpty()) {
                        return parent;
                    }
                    // Otherwise the header is in a separate block; look for the next sibling
                    // that holds the actual content.
                    Element next = parent.nextElementSibling();
                    if (next != null && !next.select("li, .informacoes-producao, .dados-producao, .cita-artigo").isEmpty()) {
                        return next;
                    }
                    // Fall back to next sibling or parent so raw-text fallbacks in callers can still run
                    if (next != null) return next;
                    return parent;
                }
                return el;
            }
        }
        // Strategy 2: look for a section by its id derived from normalized title
        String normalized = sectionTitle.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");
        Element byId = doc.getElementById(normalized);
        if (byId != null) return byId;

        return null;
    }
}
