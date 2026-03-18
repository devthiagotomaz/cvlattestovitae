package br.com.cvlattestovitae.service;

import br.com.cvlattestovitae.model.Curriculo;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

/**
 * Renders a {@link Curriculo} to a PDF byte array using
 * Thymeleaf (HTML template) and Flying Saucer (XHTML → PDF).
 */
@Service
public class PdfGeneratorService {

    private final SpringTemplateEngine templateEngine;

    public PdfGeneratorService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        this.templateEngine = new SpringTemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public byte[] generatePdf(Curriculo curriculo) throws Exception {
        // Thymeleaf context
        Context ctx = new Context(Locale.forLanguageTag("pt-BR"));
        ctx.setVariable("curriculo", curriculo);

        // Render template to HTML string
        String html = templateEngine.process("curriculo", ctx);

        // Flying Saucer: XHTML → PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        }
    }
}
