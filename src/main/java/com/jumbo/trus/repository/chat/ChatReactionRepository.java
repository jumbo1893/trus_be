package com.jumbo.trus.repository.chat;

import com.jumbo.trus.entity.chat.ChatReaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ChatReactionRepository extends JpaRepository<ChatReaction, Long> {
    @EntityGraph(attributePaths = "user")
    List<ChatReaction> findByMessageIdIn(Collection<Long> messageIds);

    @EntityGraph(attributePaths = "user")
    List<ChatReaction> findByMessageId(Long messageId);

    Optional<ChatReaction> findByMessageIdAndUserId(Long messageId, Long userId);
}
