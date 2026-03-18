package br.com.cvlattestovitae.controller;

import br.com.cvlattestovitae.model.Curriculo;
import br.com.cvlattestovitae.service.LattesParserService;
import br.com.cvlattestovitae.service.PdfGeneratorService;
import br.com.cvlattestovitae.service.WordGeneratorService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class CurriculoController {

    private final LattesParserService parserService;
    private final PdfGeneratorService pdfService;
    private final WordGeneratorService wordService;

    public CurriculoController(LattesParserService parserService, PdfGeneratorService pdfService,
                                WordGeneratorService wordService) {
        this.parserService = parserService;
        this.pdfService = pdfService;
        this.wordService = wordService;
    }

    /**
     * POST /api/parse
     * Receives the Lattes XML and returns parsed data as JSON for frontend preview.
     */
    @PostMapping(value = "/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Curriculo> parse(@RequestParam("file") MultipartFile file) {
        validateFile(file);
        try {
            Curriculo curriculo = parserService.parse(file.getInputStream());
            return ResponseEntity.ok(curriculo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/convert
     * Receives the Lattes XML and returns a downloadable PDF file.
     */
    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> convert(@RequestParam("file") MultipartFile file) {
        validateFile(file);
        try {
            Curriculo curriculo = parserService.parse(file.getInputStream());
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
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * POST /api/convert/word
     * Receives the Lattes XML and returns a downloadable Word (.docx) file.
     */
    @PostMapping(value = "/convert/word", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> convertWord(@RequestParam("file") MultipartFile file) {
        validateFile(file);
        try {
            Curriculo curriculo = parserService.parse(file.getInputStream());
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
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo não enviado ou está vazio.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".xml")) {
            throw new IllegalArgumentException("Apenas arquivos .xml são aceitos.");
        }
        // Max 10 MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("Arquivo muito grande. Limite: 10 MB.");
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) return "curriculo";
        return name.replaceAll("[^a-zA-Z0-9À-ÿ\\s_-]", "")
                .replaceAll("\\s+", "_")
                .substring(0, Math.min(name.length(), 50));
    }
}
