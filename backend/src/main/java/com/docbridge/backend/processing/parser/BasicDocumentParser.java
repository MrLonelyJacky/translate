package com.docbridge.backend.processing.parser;

import com.docbridge.backend.processing.model.ParsedDocument;
import org.springframework.stereotype.Component;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Component
public class BasicDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parse(String sourceType, String filename, byte[] content) {
        if ("JPG".equalsIgnoreCase(sourceType) || "PNG".equalsIgnoreCase(sourceType)) {
            return new ParsedDocument(true, Collections.emptyList());
        }

        String normalizedType = sourceType == null ? "" : sourceType.toUpperCase(Locale.ROOT);
        return switch (normalizedType) {
            case "PDF" -> parsePdf(content, filename);
            case "DOCX" -> parseDocx(content, filename);
            case "PPTX" -> parsePptx(content, filename);
            default -> parsePlainText(content, filename, false);
        };
    }

    private ParsedDocument parsePdf(byte[] content, String filename) {
        try (PDDocument document = Loader.loadPDF(content)) {
            String text = new PDFTextStripper().getText(document);
            boolean shouldOcr = text == null || text.trim().isBlank();
            return parsePlainText(contentFromText(text, filename), filename, shouldOcr);
        } catch (Exception ex) {
            // If parsing fails for PDF, route to OCR path as a graceful fallback for MVP.
            return new ParsedDocument(true, Collections.emptyList());
        }
    }

    private ParsedDocument parseDocx(byte[] content, String filename) {
        try (XWPFDocument document = new XWPFDocument(new java.io.ByteArrayInputStream(content))) {
            StringBuilder builder = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String line = paragraph.getText();
                if (line != null && !line.isBlank()) {
                    builder.append(line).append('\n');
                }
            }
            return parsePlainText(contentFromText(builder.toString(), filename), filename, false);
        } catch (Exception ex) {
            return parsePlainText(content, filename, false);
        }
    }

    private ParsedDocument parsePptx(byte[] content, String filename) {
        try (XMLSlideShow slideShow = new XMLSlideShow(new java.io.ByteArrayInputStream(content))) {
            StringBuilder builder = new StringBuilder();
            for (XSLFSlide slide : slideShow.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String shapeText = textShape.getText();
                        if (shapeText != null && !shapeText.isBlank()) {
                            builder.append(shapeText).append('\n');
                        }
                    }
                }
            }
            return parsePlainText(contentFromText(builder.toString(), filename), filename, false);
        } catch (Exception ex) {
            return parsePlainText(content, filename, false);
        }
    }

    private ParsedDocument parsePlainText(byte[] content, String filename, boolean ocrRequired) {
        String text = new String(content, StandardCharsets.UTF_8).trim();
        if (text.isBlank() && !ocrRequired) {
            text = "Document " + filename + " extracted text placeholder.";
        }

        List<String> segments = Arrays.stream(text.split("[\\r\\n]+"))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .limit(50)
            .toList();

        if (segments.isEmpty() && !ocrRequired) {
            segments = List.of("Document content placeholder.");
        }
        return new ParsedDocument(ocrRequired, segments);
    }

    private byte[] contentFromText(String text, String filename) {
        String value = text == null ? "Document " + filename + " extracted text placeholder." : text;
        return value.getBytes(StandardCharsets.UTF_8);
    }
}

