package io.lvoxx.ssurl.common.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.domain.audit.BaseCAtCByUAtUByEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
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
    @Builder.Default
    private boolean isActive = true;

    @Column("click_count")
    @Builder.Default
    private long clickCount = 0l;

    @Column("expire_at")
    private LocalDateTime expireAt;

}
