package com.jumbo.trus.entity.chat;

import com.jumbo.trus.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "chat_reaction", uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}))
@Getter @Setter
public class ChatReaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    @Column(nullable = false, length = 16)
    private String emoji;
}
