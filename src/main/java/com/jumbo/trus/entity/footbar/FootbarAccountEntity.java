package com.jumbo.trus.entity.footbar;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "footbar_account")
@Data
@NoArgsConstructor
public class FootbarAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UserEntity user;

    @Column(unique = true)
    private Long footbarUserId;

    private String accessToken;
    private String refreshToken;
    private Long tokenExpiry;

    private Instant linkedAt;
    private Instant lastSyncAt;

    @OneToMany
    private List<FootbarSessionEntity> sessions;

}
