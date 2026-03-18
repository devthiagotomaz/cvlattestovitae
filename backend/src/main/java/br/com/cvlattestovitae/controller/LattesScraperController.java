package br.com.cvlattestovitae.controller;

import br.com.cvlattestovitae.dto.ScraperRequest;
import br.com.cvlattestovitae.exception.CaptchaRequiredException;
import br.com.cvlattestovitae.model.Curriculo;
import br.com.cvlattestovitae.service.LattesScraperService;
import br.com.cvlattestovitae.service.PdfGeneratorService;
import br.com.cvlattestovitae.service.WordGeneratorService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/scrape")
public class LattesScraperController {

    private final LattesScraperService scraperService;
    private final PdfGeneratorService pdfService;
    private final WordGeneratorService wordService;

    public LattesScraperController(LattesScraperService scraperService,
                                   PdfGeneratorService pdfService,
                                   WordGeneratorService wordService) {
        this.scraperService = scraperService;
        this.pdfService = pdfService;
        this.wordService = wordService;
    }

    /**
     * POST /api/scrape
     * Fetches the public Lattes page and returns the parsed curriculum as JSON
     * (same contract as POST /api/parse, but accepts a URL instead of a file).
     * Returns HTTP 503 with a JSON body containing {@code captchaUrl} when the
     * Lattes page requires CAPTCHA resolution.
     */
    @PostMapping
    public ResponseEntity<?> scrape(@RequestBody ScraperRequest request) {
        try {
            scraperService.validateUrl(request.getUrl());
            Curriculo curriculo = scraperService.scrape(request.getUrl());
            return ResponseEntity.ok(curriculo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (CaptchaRequiredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "error", "captcha_required",
                            "captchaUrl", e.getCaptchaUrl(),
                            "message", "A página do Lattes requer verificação de segurança (CAPTCHA)."
                    ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/scrape/convert
     * Fetches the public Lattes page and returns a downloadable PDF.
     */
    @PostMapping("/convert")
    public ResponseEntity<byte[]> scrapeAndConvertPdf(@RequestBody ScraperRequest request) {
        try {
            scraperService.validateUrl(request.getUrl());
            Curriculo curriculo = scraperService.scrape(request.getUrl());
            byte[] pdf = pdfService.generatePdf(curriculo);

            String fileName = sanitizeFileName(curriculo.getNomeCompleto()) + "_curriculo.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build());
            headers.setContentLength(pdf.length);

            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (CaptchaRequiredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/scrape/convert/word
     * Fetches the public Lattes page and returns a downloadable Word (.docx) file.
     */
    @PostMapping("/convert/word")
    public ResponseEntity<byte[]> scrapeAndConvertWord(@RequestBody ScraperRequest request) {
        try {
            scraperService.validateUrl(request.getUrl());
            Curriculo curriculo = scraperService.scrape(request.getUrl());
            byte[] docx = wordService.generateWord(curriculo);

            String fileName = sanitizeFileName(curriculo.getNomeCompleto()) + "_curriculo.docx";
            MediaType wordType = MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(wordType);
            headers.setContentDisposition(
                    ContentDisposition.attachment()
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build());
            headers.setContentLength(docx.length);

            return ResponseEntity.ok().headers(headers).body(docx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (CaptchaRequiredException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "curriculo";
        String sanitized = name.replaceAll("[^a-zA-Z0-9À-ÿ\\s_-]", "")
                .replaceAll("\\s+", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 50));
    }
}
