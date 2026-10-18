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
                                  @Value("${app.quota.free-total:3}") int freeTotal,
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
                quota.setFreeTotal(freeTotal);
                quota.setFreeUsed(0);
                quota.setFreeRefunded(0);
                quota.setUpdatedAt(LocalDateTime.now());
                quotaAccountMapper.insert(quota);
            } else if (existingQuota.getFreeTotal() == null || existingQuota.getFreeTotal() != freeTotal) {
                existingQuota.setFreeTotal(freeTotal);
                existingQuota.setUpdatedAt(LocalDateTime.now());
                quotaAccountMapper.updateById(existingQuota);
            }
        };
    }
}

