package io.lvoxx.ssurl.common.model.audit;

import io.lvoxx.ssurl.common.util.Constants;
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
public abstract class BaseCAtCByEntity extends BaseCAtEntity {
    @Column(Constants.Columns.CREATED_BY)
    @Builder.Default
    private String createdBy = Constants.Defaults.CREATED_BY;
}
