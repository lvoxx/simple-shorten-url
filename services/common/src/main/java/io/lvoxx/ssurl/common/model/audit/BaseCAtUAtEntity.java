package io.lvoxx.ssurl.common.model.audit;

import io.lvoxx.ssurl.common.util.Constants;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
public abstract class BaseCAtUAtEntity extends BaseCAtEntity {
    @Column(Constants.Columns.UPDATED_AT)
    @Builder.Default
    @LastModifiedDate
    private LocalDateTime updatedAt = LocalDateTime.now();
}
