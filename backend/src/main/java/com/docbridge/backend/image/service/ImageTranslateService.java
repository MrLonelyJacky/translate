package com.docbridge.backend.image.service;

import com.docbridge.backend.common.exception.BusinessException;
import com.docbridge.backend.image.dto.ImageOcrLine;
import com.docbridge.backend.image.dto.ImageOcrResponse;
import com.docbridge.backend.image.dto.ImageTranslateLine;
import com.docbridge.backend.image.dto.ImageTranslateResponse;
import com.docbridge.backend.processing.model.OcrLine;
import com.docbridge.backend.processing.ocr.OcrService;
import com.docbridge.backend.processing.translate.TranslatorService;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import com.docbridge.backend.quota.mapper.QuotaAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ImageTranslateService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    private final OcrService ocrService;
    private final TranslatorService translatorService;
    private final QuotaAccountMapper quotaAccountMapper;

    public ImageTranslateService(OcrService ocrService,
                                 TranslatorService translatorService,
                                 QuotaAccountMapper quotaAccountMapper) {
        this.ocrService = ocrService;
        this.translatorService = translatorService;
        this.quotaAccountMapper = quotaAccountMapper;
    }

    public ImageOcrResponse ocr(Long userId, MultipartFile file) {
        consumeOcrQuota(userId);
        validateImage(file);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception ex) {
            throw new BusinessException("BAD_REQUEST", "读取上传文件失败", HttpStatus.BAD_REQUEST);
        }

        String sourceType = sourceTypeFromFilename(file.getOriginalFilename());
        List<OcrLine> ocrLines = ocrService.extractLines(sourceType, content);
        if (ocrLines.isEmpty()) {
            return new ImageOcrResponse(0, "", List.of());
        }

        List<ImageOcrLine> lines = new ArrayList<>(ocrLines.size());
        StringBuilder recognized = new StringBuilder();
        for (int i = 0; i < ocrLines.size(); i++) {
            OcrLine line = ocrLines.get(i);
            lines.add(new ImageOcrLine(
                line.order(),
                line.text(),
                line.left(),
                line.top(),
                line.right(),
                line.bottom()
            ));
            if (line.text() != null && !line.text().isBlank()) {
                if (recognized.length() > 0) {
                    recognized.append('\n');
                }
                recognized.append(line.text());
            }
        }
        return new ImageOcrResponse(lines.size(), recognized.toString(), lines);
    }

    public ImageTranslateResponse translateText(Long userId, String text) {
        consumeTranslateQuota(userId);
        if (text == null || text.isBlank()) {
            throw new BusinessException("BAD_REQUEST", "待翻译文本不能为空", HttpStatus.BAD_REQUEST);
        }

        List<String> sourceLines = Arrays.stream(text.split("\\R", -1)).toList();
        boolean hasNonBlank = sourceLines.stream().anyMatch(line -> line != null && !line.isBlank());
        if (!hasNonBlank) {
            throw new BusinessException("BAD_REQUEST", "待翻译文本不能为空", HttpStatus.BAD_REQUEST);
        }

        List<String> translated = translatorService.translateBatch(sourceLines);
        if (translated.size() != sourceLines.size() && translated.size() > sourceLines.size() && !sourceLines.isEmpty()) {
                StringBuilder mergedTail = new StringBuilder();
                for (int i = sourceLines.size(); i < translated.size(); i++) {
                    if (!mergedTail.isEmpty()) {
                        mergedTail.append('\n');
                    }
                    mergedTail.append(translated.get(i));
                }
                int lastIndex = sourceLines.size() - 1;
                String last = translated.get(lastIndex);
                translated = new ArrayList<>(translated.subList(0, sourceLines.size()));
                translated.set(lastIndex, (last == null ? "" : last) + "\n" + mergedTail);
            }


        List<ImageTranslateLine> lines = new ArrayList<>(sourceLines.size());
        for (int i = 0; i < sourceLines.size(); i++) {
            String translatedText = i < translated.size() ? translated.get(i) : "";
            lines.add(new ImageTranslateLine(
                i + 1,
                sourceLines.get(i),
                translatedText == null ? "" : translatedText,
                null,
                null,
                null,
                null
            ));
        }
        return new ImageTranslateResponse(lines.size(), lines);
    }

    private void consumeOcrQuota(Long userId) {
        consumeQuota(userId, "OCR");
    }

    private void consumeTranslateQuota(Long userId) {
        consumeQuota(userId, "TRANSLATE");
    }

    private void consumeQuota(Long userId, String dimension) {
        QuotaAccountEntity quota = quotaAccountMapper.selectById(userId);
        if (quota == null) {
            throw new BusinessException("QUOTA_NOT_FOUND", "配额信息不存在", HttpStatus.FORBIDDEN);
        }

        int total;
        int used;
        int refunded;
        if ("OCR".equals(dimension)) {
            total = quota.getOcrTotal() == null ? 0 : quota.getOcrTotal();
            used = quota.getOcrUsed() == null ? 0 : quota.getOcrUsed();
            refunded = quota.getOcrRefunded() == null ? 0 : quota.getOcrRefunded();
        } else {
            total = quota.getTranslateTotal() == null ? 0 : quota.getTranslateTotal();
            used = quota.getTranslateUsed() == null ? 0 : quota.getTranslateUsed();
            refunded = quota.getTranslateRefunded() == null ? 0 : quota.getTranslateRefunded();
        }

        int remaining = total + refunded - used;
        if (remaining <= 0) {
            throw new BusinessException("QUOTA_EXHAUSTED", dimension + " 配额已用尽", HttpStatus.FORBIDDEN);
        }
        if ("OCR".equals(dimension)) {
            quota.setOcrUsed(used + 1);
        } else {
            quota.setTranslateUsed(used + 1);
        }
        quota.setUpdatedAt(LocalDateTime.now());
        quotaAccountMapper.updateById(quota);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("BAD_REQUEST", "文件不能为空", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("FILE_TOO_LARGE", "图片大小超过 10MB 限制", HttpStatus.BAD_REQUEST);
        }
        String sourceType = sourceTypeFromFilename(file.getOriginalFilename());
        if (!List.of("JPG", "PNG").contains(sourceType)) {
            throw new BusinessException("UNSUPPORTED_FORMAT", "仅支持 JPG/PNG 图片", HttpStatus.BAD_REQUEST);
        }
    }

    private String sourceTypeFromFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "UNKNOWN";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
        return switch (ext) {
            case "JPG", "PNG" -> ext;
            case "JPEG" -> "JPG";
            default -> "UNKNOWN";
        };
    }
}
