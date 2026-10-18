package com.docbridge.backend.quota.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("quota_account")
public class QuotaAccountEntity {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;
    private Integer freeTotal;
    private Integer freeUsed;
    private Integer freeRefunded;
    private LocalDateTime updatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getFreeTotal() {
        return freeTotal;
    }

    public void setFreeTotal(Integer freeTotal) {
        this.freeTotal = freeTotal;
    }

    public Integer getFreeUsed() {
        return freeUsed;
    }

    public void setFreeUsed(Integer freeUsed) {
        this.freeUsed = freeUsed;
    }

    public Integer getFreeRefunded() {
        return freeRefunded;
    }

    public void setFreeRefunded(Integer freeRefunded) {
        this.freeRefunded = freeRefunded;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

