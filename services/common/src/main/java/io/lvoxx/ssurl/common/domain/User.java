package io.lvoxx.ssurl.common.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import io.lvoxx.ssurl.common.domain.audit.BaseCAtUAtEntity;
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
@Table("users")
public class User extends BaseCAtUAtEntity {

    @Id
    private Long id;

    private String username;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    @Builder.Default
    private String role = "USER";

    @Column("is_active")
    @Builder.Default
    private boolean isActive = true;

}
