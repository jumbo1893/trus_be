package com.jumbo.trus.entity.achievement;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jumbo.trus.entity.MatchEntity;
import com.jumbo.trus.entity.PlayerEntity;
import com.jumbo.trus.entity.football.FootballMatchEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Objects;

@Entity
@Table(
        name = "player_achievement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "achievement_id"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlayerAchievementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "player_achievement_seq")
    @SequenceGenerator(name = "player_achievement_seq", sequenceName = "player_achievement_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private AchievementEntity achievement;

    @ManyToOne
    private PlayerEntity player;

    @ManyToOne
    private MatchEntity match;

    @ManyToOne
    private FootballMatchEntity footballMatch;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private Boolean accomplished;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Prague")
    private Date accomplishedDate;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerAchievementEntity that = (PlayerAchievementEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
