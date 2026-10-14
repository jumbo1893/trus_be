package com.jumbo.trus.repository.chat;

import com.jumbo.trus.entity.chat.ChatRead;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChatReadRepository extends JpaRepository<ChatRead, Long> {
    Optional<ChatRead> findByAppTeamIdAndUserIdAndRoom(Long teamId, Long userId, String room);
    List<ChatRead> findByAppTeamIdAndRoom(Long teamId, String room);
}
