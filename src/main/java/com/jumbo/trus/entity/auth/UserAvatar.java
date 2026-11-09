package com.jumbo.trus.entity.auth;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_avatar")
@Data
public class UserAvatar {
    @Id
    private Long userId;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 2_000_000)
    private byte[] image;
}
