package com.docbridge.backend.quota.service;

import com.docbridge.backend.quota.mapper.QuotaAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class QuotaResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuotaResetScheduler.class);

    private final QuotaAccountMapper quotaAccountMapper;
    private final int monthlyFreeTotal;

    public QuotaResetScheduler(QuotaAccountMapper quotaAccountMapper,
                               @Value("${app.quota.monthly-free-total:1000}") int monthlyFreeTotal) {
        this.quotaAccountMapper = quotaAccountMapper;
        this.monthlyFreeTotal = monthlyFreeTotal;
    }

    @Scheduled(
        cron = "${app.quota.monthly-reset-cron:0 0 0 1 * ?}",
        zone = "${app.quota.monthly-reset-zone:Asia/Shanghai}"
    )
    public void resetMonthlyQuota() {
        int affected = quotaAccountMapper.resetAllMonthlyQuota(monthlyFreeTotal, LocalDateTime.now());
        log.info("Monthly quota reset done. freeTotal={}, affectedRows={}", monthlyFreeTotal, affected);
    }
}

