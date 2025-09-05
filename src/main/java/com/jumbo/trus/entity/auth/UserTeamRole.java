package com.jumbo.trus.entity.auth;

import com.jumbo.trus.entity.PlayerEntity;
import jakarta.persistence.*;
import lombok.Data;

import java.util.Objects;

@Entity
@Table(name = "user_team_role")
@Data
public class UserTeamRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne
    @JoinColumn(name = "app_team_id", referencedColumnName = "id", nullable = false)
    private AppTeamEntity appTeam;

    @Column(nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "player_id")
    private PlayerEntity player;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTeamRole that = (UserTeamRole) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}