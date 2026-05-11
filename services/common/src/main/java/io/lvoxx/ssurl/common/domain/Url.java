package io.lvoxx.ssurl.common.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.domain.audit.BaseCAtCByUAtUByEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table("urls")
public class Url extends BaseCAtCByUAtUByEntity{

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    @Column("original_url")
    private String originalUrl;

    @Column("user_id")
    private Long userId;

    private String title;

    @Column("is_active")
    private boolean isActive;

    @Column("click_count")
    private long clickCount;

    @Column("expire_at")
    private LocalDateTime expireAt;

}
