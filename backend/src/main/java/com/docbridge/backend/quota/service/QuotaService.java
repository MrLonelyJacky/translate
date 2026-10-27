package com.docbridge.backend.quota.service;

import com.docbridge.backend.quota.dto.QuotaResponse;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import com.docbridge.backend.quota.mapper.QuotaAccountMapper;
import org.springframework.stereotype.Service;

@Service
public class QuotaService {

    private final QuotaAccountMapper quotaAccountMapper;

    public QuotaService(QuotaAccountMapper quotaAccountMapper) {
        this.quotaAccountMapper = quotaAccountMapper;
    }

    public QuotaResponse getMyQuota(Long userId) {
        QuotaAccountEntity quota = quotaAccountMapper.selectById(userId);
        if (quota == null) {
            return new QuotaResponse(0, 0, 0, 0, 0, 0, 0, 0);
        }
        int ocrTotal = valueOrZero(quota.getOcrTotal());
        int ocrUsed = valueOrZero(quota.getOcrUsed());
        int ocrRefunded = valueOrZero(quota.getOcrRefunded());
        int translateTotal = valueOrZero(quota.getTranslateTotal());
        int translateUsed = valueOrZero(quota.getTranslateUsed());
        int translateRefunded = valueOrZero(quota.getTranslateRefunded());
        return new QuotaResponse(
            ocrTotal,
            ocrUsed,
            ocrRefunded,
            ocrTotal + ocrRefunded - ocrUsed,
            translateTotal,
            translateUsed,
            translateRefunded,
            translateTotal + translateRefunded - translateUsed
        );
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
