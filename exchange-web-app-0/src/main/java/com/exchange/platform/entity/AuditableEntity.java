package com.exchange.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

/**
 * 可稽核實體類別
 * 延伸 BaseEntity，新增 createdBy 與 updatedBy 欄位供需要稽核的實體使用
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AuditableEntity extends BaseEntity {
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private Long createdBy;
    
    @LastModifiedBy
    @Column(name = "updated_by")
    private Long updatedBy;
}
