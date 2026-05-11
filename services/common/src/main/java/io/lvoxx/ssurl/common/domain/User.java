package io.lvoxx.ssurl.common.domain;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Table("users")
public class User extends BaseCAtUAtEntity {

    @Id
    private Long id;

    private String username;

    private String email;

    private String password;

    private String role;

    @Column("is_active")
    private boolean isActive;

}
