package com.docbridge.backend.quota.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("quota_account")
public class QuotaAccountEntity {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Integer ocrTotal;
    private Integer ocrUsed;
    private Integer ocrRefunded;
    private Integer translateTotal;
    private Integer translateUsed;
    private Integer translateRefunded;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getOcrTotal() {
        return ocrTotal;
    }

    public void setOcrTotal(Integer ocrTotal) {
        this.ocrTotal = ocrTotal;
    }

    public Integer getOcrUsed() {
        return ocrUsed;
    }

    public void setOcrUsed(Integer ocrUsed) {
        this.ocrUsed = ocrUsed;
    }

    public Integer getOcrRefunded() {
        return ocrRefunded;
    }

    public void setOcrRefunded(Integer ocrRefunded) {
        this.ocrRefunded = ocrRefunded;
    }

    public Integer getTranslateTotal() {
        return translateTotal;
    }

    public void setTranslateTotal(Integer translateTotal) {
        this.translateTotal = translateTotal;
    }

    public Integer getTranslateUsed() {
        return translateUsed;
    }

    public void setTranslateUsed(Integer translateUsed) {
        this.translateUsed = translateUsed;
    }

    public Integer getTranslateRefunded() {
        return translateRefunded;
    }

    public void setTranslateRefunded(Integer translateRefunded) {
        this.translateRefunded = translateRefunded;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
