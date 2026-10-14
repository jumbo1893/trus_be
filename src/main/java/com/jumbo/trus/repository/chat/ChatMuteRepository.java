package com.jumbo.trus.repository.chat;

import com.jumbo.trus.entity.chat.ChatMute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMuteRepository extends JpaRepository<ChatMute, Long> {
    Optional<ChatMute> findByAppTeamIdAndUserIdAndRoom(Long teamId, Long userId, String room);
}
