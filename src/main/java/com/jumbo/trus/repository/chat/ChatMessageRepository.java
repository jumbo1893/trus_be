package com.jumbo.trus.repository.chat;

import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.service.chat.ChatContextLine;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByAppTeamIdAndRoomOrderByIdDesc(Long teamId, String room, Pageable pageable);
    List<ChatMessage> findByAppTeamIdAndRoomAndIdLessThanOrderByIdDesc(Long teamId, String room, Long before, Pageable pageable);
    @Query("select new com.jumbo.trus.service.chat.ChatContextLine(m.authorName, m.text, m.createdAt) from ChatMessage m where m.appTeam.id = :teamId and m.room = :room and m.id <= :messageId and m.text is not null order by m.id desc")
    List<ChatContextLine> findRecentText(Long teamId, String room, Long messageId, Pageable pageable);
    @Query("select count(m) from ChatMessage m where m.appTeam.id = :teamId and m.room = :room and m.id > :id and (m.bot = true or m.author.id <> :authorId)")
    long countUnreadAfter(Long teamId, String room, Long id, Long authorId);
    @Query("select count(m) from ChatMessage m where m.appTeam.id = :teamId and m.room = :room and (m.bot = true or m.author.id <> :authorId)")
    long countUnread(Long teamId, String room, Long authorId);
}
