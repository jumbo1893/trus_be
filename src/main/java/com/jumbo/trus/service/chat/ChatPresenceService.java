package com.jumbo.trus.service.chat;

import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatReadRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.header.HeaderManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;

@Service @RequiredArgsConstructor
public class ChatPresenceService {
    private static final Duration EXPIRY = Duration.ofSeconds(45);
    private final UserService users;
    private final HeaderManager headers;
    private final UserTeamRoleRepository roles;
    private final ChatReadRepository reads;
    private final ChatMessageRepository messages;
    private final ConcurrentHashMap<Session, Instant> active = new ConcurrentHashMap<>();

    public record OnlineUser(Long id, String name) {}
    public record Member(Long id, String name, boolean online, Instant lastReadMessageAt) {}
    private record Session(Long teamId, Long userId, String sessionId) {}

    public void heartbeat(String sessionId, boolean online) {
        if (sessionId == null || !sessionId.matches("[A-Za-z0-9-]{1,64}"))
            throw new IllegalArgumentException("Neplatné ID relace");
        Long teamId = headers.getAppTeamIdHeader();
        Long userId = users.getCurrentUserEntity().getId();
        if (teamId == null || roles.findByUserIdAndAppTeamId(userId, teamId).isEmpty())
            throw new SecurityException("Nejste členem týmu");
        Session session = new Session(teamId, userId, sessionId);
        Instant now = Instant.now();
        if (online) active.put(session, now.plus(EXPIRY));
        else active.remove(session);
        prune();
    }

    public List<OnlineUser> online(String kind) {
        ChatService.room(kind);
        Long teamId = headers.getAppTeamIdHeader();
        Long userId = users.getCurrentUserEntity().getId();
        var requester = teamId == null ? null : roles.findByUserIdAndAppTeamId(userId, teamId).orElse(null);
        if (requester == null || "players".equals(kind) && !player(requester))
            throw new SecurityException("Nemáte přístup k chatu");
        prune();
        return roles.findAllByAppTeamId(teamId).stream()
                .filter(role -> !role.getUser().getId().equals(userId))
                .filter(role -> "all".equals(kind) || player(role))
                .filter(role -> active.keySet().stream().anyMatch(session ->
                        session.teamId().equals(teamId) && session.userId().equals(role.getUser().getId())))
                .map(role -> {
                    String name = "players".equals(kind) ? role.getPlayer().getName() : role.getUser().getName();
                    if (name == null || name.isBlank()) name = role.getUser().getMail();
                    if (name == null || name.isBlank()) name = "Uživatel";
                    return new OnlineUser(role.getUser().getId(), name);
                })
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    public List<Member> members(String kind) {
        ChatService.room(kind);
        Long teamId = headers.getAppTeamIdHeader();
        Long userId = users.getCurrentUserEntity().getId();
        var requester = teamId == null ? null : roles.findByUserIdAndAppTeamId(userId, teamId).orElse(null);
        if (requester == null || "players".equals(kind) && !player(requester))
            throw new SecurityException("Nemáte přístup k chatu");
        prune();
        var readByUser = reads.findByAppTeamIdAndRoom(teamId, kind).stream()
                .collect(java.util.stream.Collectors.toMap(read -> read.getUser().getId(), read -> read));
        Map<Long, Instant> messageTimes = messages.findAllById(readByUser.values().stream()
                        .map(read -> read.getLastMessageId()).filter(Objects::nonNull).toList()).stream()
                .filter(message -> message.getAppTeam().getId().equals(teamId) && message.getRoom().equals(kind))
                .collect(Collectors.toMap(message -> message.getId(), message -> message.getCreatedAt()));
        return roles.findAllByAppTeamId(teamId).stream()
                .filter(role -> "all".equals(kind) || player(role))
                .map(role -> {
                    Long id = role.getUser().getId();
                    String name = "players".equals(kind) ? role.getPlayer().getName() : role.getUser().getName();
                    if (name == null || name.isBlank()) name = role.getUser().getMail();
                    if (name == null || name.isBlank()) name = "Uživatel";
                    boolean isOnline = active.keySet().stream().anyMatch(session ->
                            session.teamId().equals(teamId) && session.userId().equals(id));
                    var read = readByUser.get(id);
                    return new Member(id, name, isOnline,
                            read == null ? null : messageTimes.get(read.getLastMessageId()));
                })
                .sorted((a, b) -> a.name().compareToIgnoreCase(b.name()))
                .toList();
    }

    private boolean player(com.jumbo.trus.entity.auth.UserTeamRole role) {
        return role.getPlayer() != null && !role.getPlayer().isFan() && !role.getPlayer().isDeleted();
    }

    private void prune() {
        Instant now = Instant.now();
        active.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
