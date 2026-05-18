package io.lvoxx.ssurl.common.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.model.audit.BaseCAtCByUAtUByEntity;
import io.lvoxx.ssurl.common.util.Constants;
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
@Table(Constants.Tables.URLS)
public class Url extends BaseCAtCByUAtUByEntity{

    @Id
    private Long id;

    @Column(Constants.Columns.SHORT_CODE)
    private String shortCode;

    @Column(Constants.Columns.ORIGINAL_URL)
    private String originalUrl;

    @Column(Constants.Columns.USER_ID)
    private Long userId;

    private String title;

    @Column(Constants.Columns.IS_ACTIVE)
    @Builder.Default
    private boolean isActive = true;

    @Column(Constants.Columns.CLICK_COUNT)
    @Builder.Default
    private long clickCount = 0l;

    @Column(Constants.Columns.EXPIRE_AT)
    private LocalDateTime expireAt;

}
