package io.lvoxx.ssurl.common.model;

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
@Table(Constants.Tables.ANALYTICS)
public class Analytics extends BaseCAtEntity {

    @Id
    private Long id;

    @Column(Constants.Columns.SHORT_CODE)
    private String shortCode;

    private String ip;

    @Column(Constants.Columns.USER_AGENT)
    private String userAgent;

    private String referer;

    private String country;

}
