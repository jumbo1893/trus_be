package com.jumbo.trus.service.chat;

import com.jumbo.trus.dto.ai.AiQuestionResponse;
import com.jumbo.trus.repository.auth.AppTeamRepository;
import com.jumbo.trus.repository.auth.UserRepository;
import com.jumbo.trus.repository.auth.UserTeamRoleRepository;
import com.jumbo.trus.repository.chat.ChatMessageRepository;
import com.jumbo.trus.service.ai.AiQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service @RequiredArgsConstructor @Slf4j
public class ChatBotService {
    private static final Pattern INVOCATION = Pattern.compile("(?iu)^trusbote(?=\\W|$)");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d. M. yyyy H:mm")
            .withZone(ZoneId.of("Europe/Prague"));
    private final UserRepository users;
    private final AppTeamRepository teams;
    private final UserTeamRoleRepository roles;
    private final ChatMessageRepository messages;
    private final AiQuestionService ai;
    private final ChatBotReplyService replies;

    public static boolean isInvocation(String text) {
        return text != null && INVOCATION.matcher(text.stripLeading()).find();
    }

    @Async
    public void answer(Long teamId, Long userId, String kind, Long messageId, String text) {
        if (!isInvocation(text)) return;
        try {
            var role = roles.findByUserIdAndAppTeamId(userId, teamId).orElse(null);
            if (role == null || "players".equals(kind) &&
                    (role.getPlayer() == null || role.getPlayer().isFan() || role.getPlayer().isDeleted())) return;
            var user = users.findById(userId).orElseThrow();
            var team = teams.findById(teamId).orElseThrow();
            List<ChatContextLine> recent = messages.findRecentText(teamId, kind, messageId, PageRequest.of(0, 80));
            String context = recentContext(recent);
            String question = text.stripLeading().substring("trusbote".length()).stripLeading()
                    .replaceFirst("^[,;:!?\\s]+", "").trim();
            if (question.isEmpty()) question = "Pozdrav účastníky chatu a zeptej se, s čím můžeš pomoci.";
            if (question.length() > 1000) question = question.substring(0, 1000);
            context += "\nAktuální dotaz včetně všech vybraných zápasů a sezon, plné znění: \"" + text + "\".\n";
            var askedMessage = messages.findWithReplyToById(messageId).orElse(null);
            var original = askedMessage == null ? null : askedMessage.getReplyTo();
            if (original != null) {
                String originalText = original.getText() == null ? "[Obrázek bez textu]" : original.getText();
                context += "\nPůvodní citovaná zpráva uživatele " + original.getAuthorName()
                        + " (id zprávy " + original.getId() + "), plné znění: \"" + originalText + "\".\n";
            }
            AiQuestionResponse response = ai.askForChat(user, team, question, context);
            replies.save(teamId, userId, kind, response.getAnswer());
        } catch (Exception e) {
            log.warn("TrusBot could not answer chat message {}", messageId, e);
            try { replies.save(teamId, userId, kind, "Odpověď se teď nepodařilo vytvořit. Zkus to prosím později."); }
            catch (Exception replyError) { log.warn("TrusBot error reply failed for message {}", messageId, replyError); }
        }
    }

    static String recentContext(List<ChatContextLine> messages) {
        List<ChatContextLine> chronological = new ArrayList<>(messages);
        Collections.reverse(chronological);
        StringBuilder context = new StringBuilder();
        for (ChatContextLine message : chronological) {
            if (message.text() == null || message.text().isBlank()) continue;
            String line = "[" + TIME.format(message.createdAt()) + "] " + message.author()
                    + ": " + message.text().replace('\n', ' ');
            if (line.length() > 600) line = line.substring(0, 600) + "…";
            if (context.length() + line.length() > 20_000) context.delete(0, Math.min(context.length(), line.length()));
            context.append(line).append('\n');
        }
        return context.toString();
    }
}
