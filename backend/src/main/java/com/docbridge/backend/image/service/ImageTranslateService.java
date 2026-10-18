package com.docbridge.backend.image.service;

import com.docbridge.backend.common.exception.BusinessException;
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

    public ImageTranslateResponse translate(Long userId, MultipartFile file) {
        consumeQuota(userId);
        validateImage(file);
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception ex) {
            throw new BusinessException("FILE_READ_FAILED", "读取上传文件失败", HttpStatus.BAD_REQUEST);
        }

        String sourceType = sourceTypeFromFilename(file.getOriginalFilename());
        List<OcrLine> ocrLines = ocrService.extractLines(sourceType, content);
        if (ocrLines.isEmpty()) {
            return new ImageTranslateResponse(0, List.of());
        }

        List<String> sources = ocrLines.stream().map(OcrLine::text).toList();
        List<String> translated = translatorService.translateBatch(sources);
        if (translated.size() != sources.size()) {
            throw new BusinessException("TRANSLATION_FAILED", "翻译结果数量不匹配", HttpStatus.BAD_GATEWAY);
        }

        List<ImageTranslateLine> lines = new ArrayList<>(ocrLines.size());
        for (int i = 0; i < ocrLines.size(); i++) {
            OcrLine line = ocrLines.get(i);
            lines.add(new ImageTranslateLine(
                line.order(),
                line.text(),
                translated.get(i),
                line.left(),
                line.top(),
                line.right(),
                line.bottom()
            ));
        }
        return new ImageTranslateResponse(lines.size(), lines);
    }

    private void consumeQuota(Long userId) {
        QuotaAccountEntity quota = quotaAccountMapper.selectById(userId);
        if (quota == null) {
            throw new BusinessException("QUOTA_NOT_FOUND", "配额信息不存在", HttpStatus.FORBIDDEN);
        }
        int total = quota.getFreeTotal() == null ? 0 : quota.getFreeTotal();
        int used = quota.getFreeUsed() == null ? 0 : quota.getFreeUsed();
        int refunded = quota.getFreeRefunded() == null ? 0 : quota.getFreeRefunded();
        int remaining = total + refunded - used;
        if (remaining <= 0) {
            throw new BusinessException("QUOTA_EXHAUSTED", "免费配额已用尽", HttpStatus.FORBIDDEN);
        }
        quota.setFreeUsed(used + 1);
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

