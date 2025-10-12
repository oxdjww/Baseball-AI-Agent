package com.kbank.baa.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@MappedSuperclass
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @org.springframework.data.annotation.CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @org.springframework.data.annotation.LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private java.time.Instant updatedAt;
}
