package com.jumbo.trus.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "app_team_join_code",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_app_team_join_code_code", columnNames = "code"),
                @UniqueConstraint(
                        name = "uk_app_team_join_code_team_role",
                        columnNames = {"app_team_id", "granted_role"}
                )
        }
)
@Getter
@Setter
public class TeamJoinCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "granted_role", nullable = false, length = 16)
    private TeamRole grantedRole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_team_id", nullable = false)
    private AppTeamEntity appTeam;
}
