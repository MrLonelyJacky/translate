package com.docbridge.backend.quota.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.docbridge.backend.quota.entity.QuotaAccountEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface QuotaAccountMapper extends BaseMapper<QuotaAccountEntity> {

	@Update("UPDATE quota_account "
        + "SET ocr_total = #{ocrTotal}, ocr_used = 0, ocr_refunded = 0, "
        + "translate_total = #{translateTotal}, translate_used = 0, translate_refunded = 0, "
        + "updated_at = #{updatedAt}")
	int resetAllMonthlyQuota(@Param("ocrTotal") int ocrTotal,
                             @Param("translateTotal") int translateTotal,
                             @Param("updatedAt") LocalDateTime updatedAt);
}
