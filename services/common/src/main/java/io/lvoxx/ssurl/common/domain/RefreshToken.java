package io.lvoxx.ssurl.common.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.domain.audit.BaseCAtEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table("refresh_tokens")
public class RefreshToken extends BaseCAtEntity {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    private String token;

    @Column("expires_at")
    private LocalDateTime expiresAt;

}
