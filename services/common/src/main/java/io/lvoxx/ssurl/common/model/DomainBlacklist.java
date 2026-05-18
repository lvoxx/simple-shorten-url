package io.lvoxx.ssurl.common.model;

import org.springframework.data.annotation.Id;
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
@Table(Constants.Tables.DOMAIN_BLACKLIST)
public class DomainBlacklist extends BaseCAtEntity {

    @Id
    private Long id;

    private String domain;

    private String reason;

}
