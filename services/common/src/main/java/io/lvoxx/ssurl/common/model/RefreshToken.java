package io.lvoxx.ssurl.common.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.model.audit.BaseCAtEntity;
import io.lvoxx.ssurl.common.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Table(Constants.Tables.REFRESH_TOKENS)
public class RefreshToken extends BaseCAtEntity {

    @Id
    private Long id;

    @Column(Constants.Columns.USER_ID)
    private Long userId;

    private String token;

    @Column(Constants.Columns.EXPIRES_AT)
    private LocalDateTime expiresAt;

}
