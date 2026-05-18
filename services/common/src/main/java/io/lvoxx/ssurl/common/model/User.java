package io.lvoxx.ssurl.common.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.model.audit.BaseCAtUAtEntity;
import io.lvoxx.ssurl.common.util.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table(Constants.Tables.USERS)
public class User extends BaseCAtUAtEntity {

    @Id
    private Long id;

    private String username;

    private String email;

    @Column(Constants.Columns.PASSWORD_HASH)
    private String passwordHash;

    @Builder.Default
    private String role = Constants.Defaults.ROLE;

    @Column(Constants.Columns.IS_ACTIVE)
    @Builder.Default
    private boolean isActive = true;

}
