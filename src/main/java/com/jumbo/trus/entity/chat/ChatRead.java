package com.jumbo.trus.entity.chat;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_read", uniqueConstraints = @UniqueConstraint(columnNames = {"app_team_id", "user_id", "room"}))
@Getter @Setter
public class ChatRead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_team_id", nullable = false)
    private AppTeamEntity appTeam;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(nullable = false, length = 16)
    private String room;
    private Long lastMessageId;
}
