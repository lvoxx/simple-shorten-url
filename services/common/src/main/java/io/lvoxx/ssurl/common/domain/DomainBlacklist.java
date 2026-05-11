package io.lvoxx.ssurl.common.domain;

import org.springframework.data.annotation.Id;
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
@Table("domain_blacklist")
public class DomainBlacklist extends BaseCAtEntity {

    @Id
    private Long id;

    private String domain;

    private String reason;

}
