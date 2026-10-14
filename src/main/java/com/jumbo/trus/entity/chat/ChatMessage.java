package com.jumbo.trus.entity.chat;

import com.jumbo.trus.entity.auth.AppTeamEntity;
import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "chat_message", indexes = @Index(name = "idx_chat_message_room", columnList = "app_team_id,room,id"))
@Getter @Setter
public class ChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "app_team_id", nullable = false)
    private AppTeamEntity appTeam;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity author;
    @Column(nullable = false, length = 16)
    private String room;
    @Column(length = 4000)
    private String text;
    @Column(columnDefinition = "boolean default false")
    private boolean bot = false;
    @Column(nullable = false)
    private String authorName;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    @Lob
    private byte[] image;
    private String imageContentType;
}
