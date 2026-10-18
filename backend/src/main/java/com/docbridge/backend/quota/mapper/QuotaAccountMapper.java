package com.docbridge.backend.quota.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface QuotaAccountMapper extends BaseMapper<QuotaAccountEntity> {

	@Update("UPDATE quota_account SET free_total = #{freeTotal}, free_used = 0, free_refunded = 0, updated_at = #{updatedAt}")
	int resetAllMonthlyQuota(@Param("freeTotal") int freeTotal, @Param("updatedAt") LocalDateTime updatedAt);
}

