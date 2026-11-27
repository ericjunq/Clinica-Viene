package br.com.clinicaviene.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/** PDFs dos documentos (CONTEXTO §9): template Thymeleaf (XHTML) → openhtmltopdf. */
@Service
@RequiredArgsConstructor
public class DocumentoPdfService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ITemplateEngine templateEngine;

    /** @param template nome em {@code templates/} sem extensão (ex.: {@code documentos/ficha-cadastro}) */
    public byte[] gerar(String template, Map<String, Object> variaveis) {
        String html = templateEngine.process(template, new Context(PT_BR, variaveis));
        try (ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            PdfRendererBuilder construtor = new PdfRendererBuilder();
            construtor.useFastMode();
            construtor.withHtmlContent(html, null);
            construtor.toStream(saida);
            construtor.run();
            return saida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o PDF", e);
        }
    }
}
