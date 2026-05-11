package io.lvoxx.ssurl.common.domain.audit;

import org.springframework.data.relational.core.mapping.Column;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public abstract class BaseCByEntity {
    @Column("created_by")
    @Builder.Default
    private String createdBy = "Annonymous";
    
}
