package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Table(name = "o_auth_state")
@Data
@NoArgsConstructor
public class OAuthStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String codeVerifier;

    private String system;

    private Date expiresIn;

    @ManyToOne
    private UserEntity user;

}
