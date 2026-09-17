package com.jumbo.trus.entity;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "season_recap", uniqueConstraints = @UniqueConstraint(columnNames = {"season_id", "user_id"}))
@Data
@org.hibernate.annotations.DynamicUpdate
public class SeasonRecapEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "season_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private SeasonEntity season;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private UserEntity user;
    @Column(nullable = false, columnDefinition = "text")
    private String snapshot;
    private Instant publishedAt;
    private Instant openedAt;
}
