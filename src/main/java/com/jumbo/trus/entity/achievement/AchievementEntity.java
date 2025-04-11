package com.jumbo.trus.entity.achievement;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "achievement")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "achievement_seq")
    @SequenceGenerator(name = "achievement_seq", sequenceName = "achievement_seq", allocationSize = 1)
    private Long id;

    private String code;

    private String name;

    private Boolean onlyForPlayers;

    private String description;

    private String secondaryCondition;

    private Boolean manually;

    @OneToMany(mappedBy = "achievement")
    private List<PlayerAchievementEntity> playerAchievements;

    public AchievementEntity(String name, String code, Boolean onlyForPlayers, String description, String secondaryCondition, Boolean manually) {
        this.code = code;
        this.name = name;
        this.onlyForPlayers = onlyForPlayers;
        this.description = description;
        this.secondaryCondition = secondaryCondition;
        this.manually = manually;
    }

    public AchievementEntity(String name, String code, Boolean onlyForPlayers, String description, Boolean manually) {
        this.code = code;
        this.name = name;
        this.onlyForPlayers = onlyForPlayers;
        this.description = description;
        this.manually = manually;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AchievementEntity that = (AchievementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
