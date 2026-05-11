package io.lvoxx.ssurl.common.domain.audit;

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
public abstract class BaseCAtCByUAtUByEntity extends BaseCAtCByEntity {
    @Column("updated_at")
    @Builder.Default
    @LastModifiedDate
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column("updated_by")
    @Builder.Default
    private String updatedBy = "Annonymous";
}
