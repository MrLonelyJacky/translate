package com.docbridge.backend.common.config;

import com.docbridge.backend.auth.entity.UserEntity;
import com.docbridge.backend.auth.mapper.UserMapper;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import com.docbridge.backend.quota.mapper.QuotaAccountMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
public class DevDataInitializer {

    @Bean
    ApplicationRunner initDevUser(@Value("${app.auth.enabled:false}") boolean authEnabled,
                                  @Value("${app.quota.ocr-total:3}") int ocrTotal,
                                  @Value("${app.quota.translate-total:3}") int translateTotal,
                                  UserMapper userMapper,
                                  QuotaAccountMapper quotaAccountMapper) {
        return args -> {
            if (authEnabled) {
                return;
            }

            final long devUserId = 1001L;
            UserEntity user = userMapper.selectById(devUserId);
            if (user == null) {
                user = new UserEntity();
                user.setId(devUserId);
                user.setWechatUnionid("dev_unionid_1001");
                user.setWechatOpenid("dev_openid_1001");
                user.setRole("USER");
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(user);
            }

            QuotaAccountEntity existingQuota = quotaAccountMapper.selectById(devUserId);
            if (existingQuota == null) {
                QuotaAccountEntity quota = new QuotaAccountEntity();
                quota.setUserId(devUserId);
                quota.setOcrTotal(ocrTotal);
                quota.setOcrUsed(0);
                quota.setOcrRefunded(0);
                quota.setTranslateTotal(translateTotal);
                quota.setTranslateUsed(0);
                quota.setTranslateRefunded(0);
                quota.setUpdatedAt(LocalDateTime.now());
                quotaAccountMapper.insert(quota);
            } else if (existingQuota.getOcrTotal() == null
                || existingQuota.getTranslateTotal() == null
                || existingQuota.getOcrTotal() != ocrTotal
                || existingQuota.getTranslateTotal() != translateTotal) {
                existingQuota.setOcrTotal(ocrTotal);
                existingQuota.setTranslateTotal(translateTotal);
                existingQuota.setUpdatedAt(LocalDateTime.now());
                quotaAccountMapper.updateById(existingQuota);
            }
        };
    }
}
