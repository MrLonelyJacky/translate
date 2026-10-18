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
            return new QuotaResponse(0, 0, 0, 0);
        }
        int total = quota.getFreeTotal();
        int used = quota.getFreeUsed();
        int refunded = quota.getFreeRefunded();
        return new QuotaResponse(total, used, refunded, total + refunded - used);
    }
}

