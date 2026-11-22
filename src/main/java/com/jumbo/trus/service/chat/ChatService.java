package com.jumbo.trus.service.chat;

import com.jumbo.trus.entity.auth.UserEntity;
import com.jumbo.trus.entity.auth.UserTeamRole;
import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.entity.chat.ChatRead;
import com.jumbo.trus.entity.chat.ChatReaction;
import com.jumbo.trus.entity.chat.ChatMute;
import com.jumbo.trus.entity.notification.push.settings.NotificationType;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.MatchRepository;
import com.jumbo.trus.repository.PlannedMatchRepository;
import com.jumbo.trus.entity.PlannedMatchEntity;
import com.jumbo.trus.entity.PlannedMatchStatus;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.repository.chat.ChatReadRepository;
import com.jumbo.trus.repository.chat.ChatReactionRepository;
import com.jumbo.trus.repository.chat.ChatMuteRepository;
import com.jumbo.trus.service.notification.settings.EnabledPushNotificationService;
import com.jumbo.trus.service.auth.UserService;
import com.jumbo.trus.service.header.HeaderManager;
import com.jumbo.trus.service.outbox.OutboxEventService;
import com.jumbo.trus.entity.outbox.OutboxEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class ChatService {
    private final UserService users;
    private final HeaderManager headers;
    private final UserTeamRoleRepository roles;
    private final MatchRepository matches;
    private final PlannedMatchRepository plannedMatches;
    private final ChatMessageRepository messages;
    private final ChatReadRepository reads;
    private final ChatReactionRepository reactions;
    private final ChatMuteRepository mutes;
    private final EnabledPushNotificationService pushSettings;
    private final SimpMessagingTemplate websocket;
    private final ChatPushService push;
    private final ChatBotService bot;
    private final OutboxEventService outboxEventService;

    public record Room(String kind, String title, long unread, boolean pushEnabled, boolean featured) {}
    public record PushPreference(String kind, boolean pushEnabled) {}
    public record Receipt(Long userId, String name, Long lastMessageId) {}
    public record Reaction(Long userId, String name, String emoji) {}
    public record ReactionUpdate(Long messageId, List<Reaction> reactions) {}
    public record Reply(Long id, String author, String text, boolean image) {}
    public record Message(Long id, Long authorId, String author, String text, Instant createdAt, boolean image,
                          List<Reaction> reactions, boolean bot, boolean system, Reply replyTo) {}
    public record Detail(List<Message> messages, List<Receipt> receipts) {}
    private static final Set<String> REACTION_EMOJIS = Set.of(
            "❤️", "😂", "😮", "😢", "😡", "👍", "😆", "🥳", "😍", "🔥", "👏", "🙏");
    private static final DateTimeFormatter ROOM_DATE = DateTimeFormatter.ofPattern("d. M. yyyy")
            .withZone(ZoneId.of("Europe/Prague"));

    private String matchRoomTitle(String name, Date date) {
        return (name == null || name.isBlank() ? "Zápas" : name)
                + (date == null ? "" : " · " + ROOM_DATE.format(date.toInstant()));
    }

    public static String room(String value) {
        if (!"all".equals(value) && !"players".equals(value)
                && !value.matches("match-[1-9][0-9]*") && !value.matches("planned-[1-9][0-9]*"))
            throw new IllegalArgumentException("Neplatný chat");
        return value;
    }

    private boolean playerRole(UserTeamRole role) {
        return role.getPlayer() != null && !role.getPlayer().isFan() && !role.getPlayer().isDeleted();
    }

    private UserTeamRole currentRole(String kind) {
        Long teamId = headers.getAppTeamIdHeader();
        UserEntity user = users.getCurrentUserEntity();
        if (teamId == null) throw new SecurityException("Chybí tým");
        UserTeamRole role = roles.findByUserIdAndAppTeamId(user.getId(), teamId)
                .orElseThrow(() -> new SecurityException("Nejste členem týmu"));
        if ("players".equals(room(kind)) && !playerRole(role)) throw new SecurityException("Nejste hráčem týmu");
        if (kind.startsWith("match-") && matches.findByIdAndAppTeamId(Long.parseLong(kind.substring(6)), teamId).isEmpty())
            throw new SecurityException("Zápas nepatří týmu");
        if (kind.startsWith("planned-") && plannedMatches.findByIdAndAppTeamId(Long.parseLong(kind.substring(8)), teamId).isEmpty())
            throw new SecurityException("Plánovaný zápas nepatří týmu");
        return role;
    }

    public void typing(String kind, String activityId, boolean typing) {
        UserTeamRole role = currentRole(kind);
        if (activityId == null || !activityId.matches("[A-Za-z0-9-]{1,64}"))
            throw new IllegalArgumentException("Neplatné ID psaní");
        Long teamId = role.getAppTeam().getId();
        Long userId = role.getUser().getId();
        websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind + "/typing",
                new ChatTypingEvent("user-" + userId + "-" + activityId,
                        userId, name(userId, teamId, kind), typing));
    }

    @Transactional(readOnly = true)
    public List<Room> rooms() {
        UserEntity user = users.getCurrentUserEntity();
        Long teamId = headers.getAppTeamIdHeader();
        UserTeamRole role = currentRole("all");
        List<Room> result = new ArrayList<>();
        result.add(new Room("all", "Hromadný chat", unread(teamId, user.getId(), "all"), pushEnabled(teamId, user, "all"), false));
        if (playerRole(role)) result.add(new Room("players", "Hráčský chat", unread(teamId, user.getId(), "players"), pushEnabled(teamId, user, "players"), false));
        Date now = new Date();
        Date nextDate = null, lastDate = null;
        String nextKind = null, lastKind = null;
        for (PlannedMatchEntity planned : plannedMatches.findAllByAppTeamIdOrderByDateDesc(teamId)) {
            String kind = "planned-" + planned.getId();
            String title = matchRoomTitle(planned.getOpponent() == null || planned.getOpponent().isBlank()
                    ? planned.getType().name() : planned.getOpponent(), planned.getDate());
            result.add(new Room(kind, title, unread(teamId, user.getId(), kind), pushEnabled(teamId, user, kind), false));
            if (planned.getStatus() == PlannedMatchStatus.SCHEDULED && planned.getDate() != null && planned.getDate().after(now)
                    && (nextDate == null || planned.getDate().before(nextDate))) {
                nextDate = planned.getDate(); nextKind = kind;
            } else if (planned.getStatus() != PlannedMatchStatus.CANCELLED && planned.getDate() != null && !planned.getDate().after(now)
                    && (lastDate == null || planned.getDate().after(lastDate))) {
                lastDate = planned.getDate(); lastKind = kind;
            }
        }
        for (var match : matches.findAllByAppTeamIdOrderByDateDesc(teamId)) {
            String kind = "match-" + match.getId();
            result.add(new Room(kind, matchRoomTitle(match.getName() == null || match.getName().isBlank() ? "Zápas " + match.getId() : match.getName(), match.getDate()),
                    unread(teamId, user.getId(), kind), pushEnabled(teamId, user, kind), false));
            if (match.getDate() != null && match.getDate().after(now)
                    && (nextDate == null || match.getDate().before(nextDate))) {
                nextDate = match.getDate(); nextKind = kind;
            } else if (match.getDate() != null && !match.getDate().after(now)
                    && (lastDate == null || match.getDate().after(lastDate))) {
                lastDate = match.getDate(); lastKind = kind;
            }
        }
        String featuredNext = nextKind, featuredLast = lastKind;
        return result.stream().map(room -> new Room(room.kind(), room.title(), room.unread(),
                room.pushEnabled(), room.kind().equals(featuredNext) || room.kind().equals(featuredLast))).toList();
    }

    private boolean pushEnabled(Long teamId, UserEntity user, String kind) {
        return pushSettings.isNotificationEnabled(user, NotificationType.CHAT)
                && mutes.findByAppTeamIdAndUserIdAndRoom(teamId, user.getId(), kind)
                .map(mute -> !mute.isMuted()).orElse(true);
    }

    @Transactional
    public PushPreference setPushEnabled(String kind, boolean enabled) {
        UserTeamRole role = currentRole(kind);
        Long teamId = role.getAppTeam().getId();
        UserEntity user = role.getUser();
        if (enabled && !pushSettings.isChatTypeEnabled(user)) {
            String other = "all".equals(kind) ? "players" : "all";
            if (mutes.findByAppTeamIdAndUserIdAndRoom(teamId, user.getId(), other).isEmpty()) {
                ChatMute otherMute = new ChatMute();
                otherMute.setAppTeam(role.getAppTeam());
                otherMute.setUser(user);
                otherMute.setRoom(other);
                otherMute.setMuted(true);
                mutes.save(otherMute);
            }
            pushSettings.enableChatType(user);
        }
        ChatMute mute = mutes.findByAppTeamIdAndUserIdAndRoom(teamId, user.getId(), kind)
                .orElseGet(() -> {
                    ChatMute created = new ChatMute();
                    created.setAppTeam(role.getAppTeam());
                    created.setUser(user);
                    created.setRoom(kind);
                    return created;
                });
        mute.setMuted(!enabled);
        mutes.save(mute);
        outboxEventService.createAuditEvent(OutboxEventType.CHAT_PUSH_CHANGED,
                user.getId(), role.getAppTeam().getId(), user.getId(),
                Map.of("room", kind, "enabled", Boolean.toString(enabled)));
        return new PushPreference(kind, pushEnabled(teamId, user, kind));
    }

    private long unread(Long teamId, Long userId, String kind) {
        Long last = reads.findByAppTeamIdAndUserIdAndRoom(teamId, userId, kind)
                .map(ChatRead::getLastMessageId).orElse(null);
        return last == null ? messages.countUnread(teamId, kind, userId)
                : messages.countUnreadAfter(teamId, kind, last, userId);
    }

    @Transactional(readOnly = true)
    public Detail detail(String kind, Long before) {
        UserTeamRole role = currentRole(kind);
        Long teamId = role.getAppTeam().getId();
        List<ChatMessage> page = before == null
                ? messages.findByAppTeamIdAndRoomOrderByIdDesc(teamId, kind, PageRequest.of(0, 50))
                : messages.findByAppTeamIdAndRoomAndIdLessThanOrderByIdDesc(teamId, kind, before, PageRequest.of(0, 50));
        Map<Long, UserTeamRole> teamRoles = teamRolesByUser(teamId);
        Map<Long, List<Reaction>> byMessage = page.isEmpty() ? Map.of() : reactions.findByMessageIdIn(
                page.stream().map(ChatMessage::getId).toList()).stream().collect(Collectors.groupingBy(
                reaction -> reaction.getMessage().getId(),
                Collectors.mapping(reaction -> toReaction(reaction, teamRoles), Collectors.toList())));
        List<Message> result = page.stream().map(message -> toMessage(message, byMessage.getOrDefault(message.getId(), List.of()))).toList();
        return new Detail(result, receipts(teamId, kind, teamRoles));
    }

    @Transactional(readOnly = true)
    public Detail around(String kind, Long messageId) {
        UserTeamRole role = currentRole(kind);
        Long teamId = role.getAppTeam().getId();
        ChatMessage target = messages.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Zpráva neexistuje"));
        if (!target.getAppTeam().getId().equals(teamId) || !target.getRoom().equals(kind))
            throw new SecurityException("Zpráva není v tomto chatu");
        List<ChatMessage> page = new ArrayList<>(messages.findByAppTeamIdAndRoomAndIdGreaterThanOrderByIdAsc(
                teamId, kind, messageId, PageRequest.of(0, 25)));
        java.util.Collections.reverse(page);
        page.add(target);
        page.addAll(messages.findByAppTeamIdAndRoomAndIdLessThanOrderByIdDesc(
                teamId, kind, messageId, PageRequest.of(0, 24)));
        Map<Long, UserTeamRole> teamRoles = teamRolesByUser(teamId);
        Map<Long, List<Reaction>> byMessage = reactions.findByMessageIdIn(
                page.stream().map(ChatMessage::getId).toList()).stream().collect(Collectors.groupingBy(
                reaction -> reaction.getMessage().getId(),
                Collectors.mapping(reaction -> toReaction(reaction, teamRoles), Collectors.toList())));
        List<Message> result = page.stream()
                .map(message -> toMessage(message, byMessage.getOrDefault(message.getId(), List.of()))).toList();
        return new Detail(result, receipts(teamId, kind, teamRoles));
    }

    @Transactional
    public Message send(String kind, String text, MultipartFile image) throws IOException {
        return send(kind, text, image, null);
    }

    @Transactional
    public Message send(String kind, String text, MultipartFile image, Long replyToId) throws IOException {
        UserTeamRole role = currentRole(kind);
        String body = text == null ? "" : text.trim();
        if (body.length() > 4000 || (body.isEmpty() && image == null)) throw new IllegalArgumentException("Zpráva je prázdná nebo příliš dlouhá");
        ChatMessage entity = new ChatMessage();
        entity.setAppTeam(role.getAppTeam());
        entity.setAuthor(role.getUser());
        entity.setRoom(kind);
        entity.setText(body.isEmpty() ? null : body);
        entity.setAuthorName(name(role.getUser().getId(), role.getAppTeam().getId(), kind));
        if (replyToId != null) {
            ChatMessage parent = messages.findById(replyToId)
                    .orElseThrow(() -> new IllegalArgumentException("Původní zpráva neexistuje"));
            if (!parent.getAppTeam().getId().equals(role.getAppTeam().getId()) || !parent.getRoom().equals(kind))
                throw new IllegalArgumentException("Původní zpráva není v tomto chatu");
            entity.setReplyTo(parent);
        }
        if (image != null) {
            if (image.isEmpty() || image.getSize() > 5 * 1024 * 1024) throw new IllegalArgumentException("Obrázek musí mít nejvýše 5 MB");
            byte[] bytes = image.getBytes();
            String type = image.getContentType();
            if (!"image/jpeg".equals(type) && !"image/png".equals(type)) throw new IllegalArgumentException("Podporovány jsou JPEG a PNG");
            try (ImageInputStream input = ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                if (input == null) throw new IllegalArgumentException("Neplatný obrázek");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Neplatný obrázek");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!("image/jpeg".equals(type) && "JPEG".equalsIgnoreCase(format))
                            && !("image/png".equals(type) && "PNG".equalsIgnoreCase(format)))
                        throw new IllegalArgumentException("Typ obrázku neodpovídá souboru");
                    if (reader.getWidth(0) * (long) reader.getHeight(0) > 20_000_000L)
                        throw new IllegalArgumentException("Obrázek má příliš velké rozlišení");
                } finally { reader.dispose(); }
            }
            entity.setImage(bytes);
            entity.setImageContentType(type);
        }
        Message dto = toMessage(messages.saveAndFlush(entity));
        Long teamId = role.getAppTeam().getId();
        Long authorId = role.getUser().getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind, dto); }
                catch (Exception e) { log.warn("Chat websocket delivery failed for message {}", dto.id(), e); }
                try { push.notifyMembers(teamId, authorId, kind, dto); }
                catch (Exception e) { log.warn("Chat push delivery failed for message {}", dto.id(), e); }
                if (ChatBotService.isInvocation(body)) {
                    try { bot.answer(teamId, authorId, kind, dto.id(), body); }
                    catch (Exception e) { log.warn("Chat bot dispatch failed for message {}", dto.id(), e); }
                }
            }
        });
        return dto;
    }

    @Transactional
    public Receipt markRead(String kind, Long messageId) {
        UserTeamRole role = currentRole(kind);
        Long teamId = role.getAppTeam().getId();
        Long userId = role.getUser().getId();
        ChatMessage message = messages.findById(messageId).orElseThrow(() -> new IllegalArgumentException("Zpráva neexistuje"));
        if (!message.getAppTeam().getId().equals(teamId) || !message.getRoom().equals(kind)) throw new SecurityException("Cizí zpráva");
        ChatRead read = reads.findByAppTeamIdAndUserIdAndRoom(teamId, userId, kind).orElseGet(() -> {
            ChatRead value = new ChatRead();
            value.setAppTeam(role.getAppTeam()); value.setUser(role.getUser()); value.setRoom(kind);
            return value;
        });
        boolean changed = read.getLastMessageId() == null || read.getLastMessageId() < messageId;
        if (changed) read.setLastMessageId(messageId);
        reads.save(read);
        Receipt dto = new Receipt(userId, name(userId, teamId, kind), read.getLastMessageId());
        if (changed) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind + "/read", dto); }
        });
        return dto;
    }

    @Transactional
    public ReactionUpdate setReaction(String kind, Long messageId, String emoji) {
        UserTeamRole role = currentRole(kind);
        Long teamId = role.getAppTeam().getId();
        Long userId = role.getUser().getId();
        ChatMessage message = messages.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Zpráva neexistuje"));
        if (!message.getAppTeam().getId().equals(teamId) || !message.getRoom().equals(kind))
            throw new SecurityException("Cizí zpráva");
        if (emoji != null && !REACTION_EMOJIS.contains(emoji))
            throw new IllegalArgumentException("Nepodporovaná reakce");

        var existing = reactions.findByMessageIdAndUserId(messageId, userId);
        boolean newlyAdded = emoji != null && existing.isEmpty();
        if (emoji == null) {
            existing.ifPresent(reactions::delete);
            reactions.flush();
        } else {
            ChatReaction reaction = existing.orElseGet(ChatReaction::new);
            reaction.setMessage(message);
            reaction.setUser(role.getUser());
            reaction.setEmoji(emoji);
            reactions.saveAndFlush(reaction);
        }
        ReactionUpdate update = new ReactionUpdate(messageId, reactions.findByMessageId(messageId).stream()
                .map(reaction -> toReaction(reaction, teamId, kind)).toList());
        Long messageAuthorId = message.getAuthor() == null ? null : message.getAuthor().getId();
        String reactorName = newlyAdded && !message.isBot() && messageAuthorId != null
                && !messageAuthorId.equals(userId) ? name(userId, teamId, kind) : null;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { websocket.convertAndSend("/topic/chat/" + teamId + "/" + kind + "/reactions", update); }
                catch (Exception e) { log.warn("Chat reaction websocket delivery failed for message {}", messageId, e); }
                if (reactorName != null) {
                    try { push.notifyReaction(teamId, messageAuthorId, userId, kind, messageId,
                            reactorName, emoji, message.getText()); }
                    catch (Exception e) { log.warn("Chat reaction push delivery failed for message {}", messageId, e); }
                }
            }
        });
        return update;
    }

    @Transactional(readOnly = true)
    public ChatMessage image(String kind, Long id) {
        UserTeamRole role = currentRole(kind);
        ChatMessage message = messages.findById(id).orElseThrow(() -> new IllegalArgumentException("Obrázek neexistuje"));
        if (!message.getAppTeam().getId().equals(role.getAppTeam().getId()) || !message.getRoom().equals(kind) || message.getImage() == null)
            throw new SecurityException("Obrázek není dostupný");
        return message;
    }

    private Message toMessage(ChatMessage message) {
        return toMessage(message, List.of());
    }

    private Message toMessage(ChatMessage message, List<Reaction> reactions) {
        ChatMessage parent = message.getReplyTo();
        Reply reply = parent == null ? null : new Reply(parent.getId(), displayAuthor(parent), parent.getText(),
                parent.getImageContentType() != null);
        return new Message(message.getId(), message.isBot() ? 0L : message.getAuthor().getId(), displayAuthor(message),
                message.getText(), message.getCreatedAt(), message.getImageContentType() != null, reactions,
                message.isBot(), message.isBot() && "Kurvíza".equals(message.getAuthorName()), reply);
    }

    private String displayAuthor(ChatMessage message) {
        if (message.isBot() || message.getAuthor() == null) return message.getAuthorName();
        String currentName = message.getAuthor().getName();
        if (currentName != null && !currentName.isBlank()) return currentName;
        String mail = message.getAuthor().getMail();
        return mail == null || mail.isBlank() ? message.getAuthorName() : mail;
    }


    private Reaction toReaction(ChatReaction reaction, Long teamId, String kind) {
        Long userId = reaction.getUser().getId();
        return new Reaction(userId, name(userId, teamId, kind), reaction.getEmoji());
    }

    private Reaction toReaction(ChatReaction reaction, Map<Long, UserTeamRole> teamRoles) {
        Long userId = reaction.getUser().getId();
        UserTeamRole role = teamRoles.get(userId);
        return new Reaction(userId, role == null ? "Uživatel" : displayName(role), reaction.getEmoji());
    }

    private String name(Long userId, Long teamId, String kind) {
        return roles.findByUserIdAndAppTeamId(userId, teamId).map(this::displayName).orElse("Uživatel");
    }

    private String displayName(UserTeamRole role) {
        String name = role.getUser().getName();
        return name == null || name.isBlank() ? role.getUser().getMail() : name;
    }

    /** Role týmu jedním dotazem (místo dotazu na každé read receipt / reakci). */
    private Map<Long, UserTeamRole> teamRolesByUser(Long teamId) {
        return roles.findAllWithUserAndPlayerByAppTeamId(teamId).stream()
                .collect(Collectors.toMap(role -> role.getUser().getId(), role -> role, (first, second) -> first));
    }

    private List<Receipt> receipts(Long teamId, String kind, Map<Long, UserTeamRole> teamRoles) {
        return reads.findByAppTeamIdAndRoom(teamId, kind).stream()
                .filter(read -> {
                    UserTeamRole role = teamRoles.get(read.getUser().getId());
                    return role != null && (!"players".equals(kind) || playerRole(role));
                })
                .map(read -> new Receipt(read.getUser().getId(),
                        displayName(teamRoles.get(read.getUser().getId())), read.getLastMessageId()))
                .toList();
    }
}
