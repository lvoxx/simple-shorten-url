package io.lvoxx.ssurl.common.domain;

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
@Table("analytics")
public class Analytics extends BaseCAtEntity {

    @Id
    private Long id;

    @Column("short_code")
    private String shortCode;

    private String ip;

    @Column("user_agent")
    private String userAgent;

    private String referer;

    private String country;

}
