package com.jumbo.trus.controller;

import com.jumbo.trus.config.security.RoleRequired;
import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.service.chat.ChatService;
import com.jumbo.trus.service.chat.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController @RequestMapping("/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;
    private final ChatPresenceService presence;

    @PostMapping("/presence")
    @RoleRequired("READER")
    public boolean presence(@RequestBody PresenceRequest request) {
        presence.heartbeat(request.sessionId(), request.online());
        return request.online();
    }

    @GetMapping("/{kind}/online")
    public List<ChatPresenceService.OnlineUser> online(@PathVariable String kind) {
        return presence.online(ChatService.room(kind));
    }

    @GetMapping("/{kind}/members")
    public List<ChatPresenceService.Member> members(@PathVariable String kind) {
        return presence.members(ChatService.room(kind));
    }

    @GetMapping("/rooms")
    public List<ChatService.Room> rooms() { return chat.rooms(); }

    @PutMapping("/{kind}/push")
    @RoleRequired("READER")
    public ChatService.PushPreference push(@PathVariable String kind, @RequestBody PushRequest request) {
        return chat.setPushEnabled(ChatService.room(kind), request.enabled());
    }

    @GetMapping("/{kind}")
    public ChatService.Detail messages(@PathVariable String kind, @RequestParam(required = false) Long before) {
        return chat.detail(ChatService.room(kind), before);
    }

    @GetMapping("/{kind}/around/{id}")
    public ChatService.Detail around(@PathVariable String kind, @PathVariable Long id) {
        return chat.around(ChatService.room(kind), id);
    }

    @PostMapping("/{kind}")
    @RoleRequired("READER")
    public ChatService.Message send(@PathVariable String kind, @RequestBody TextRequest request) throws IOException {
        return chat.send(ChatService.room(kind), request.text(), null, request.replyToId());
    }

    @PostMapping("/{kind}/typing")
    @RoleRequired("READER")
    public boolean typing(@PathVariable String kind, @RequestBody TypingRequest request) {
        chat.typing(ChatService.room(kind), request.activityId(), request.typing());
        return request.typing();
    }

    @PostMapping(value = "/{kind}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RoleRequired("READER")
    public ChatService.Message image(@PathVariable String kind, @RequestPart("image") MultipartFile image,
                                     @RequestParam(required = false) String text,
                                     @RequestParam(required = false) Long replyToId) throws IOException {
        return chat.send(ChatService.room(kind), text, image, replyToId);
    }

    @GetMapping("/{kind}/image/{id}")
    public ResponseEntity<byte[]> image(@PathVariable String kind, @PathVariable Long id) {
        ChatMessage message = chat.image(ChatService.room(kind), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(message.getImageContentType()))
                .cacheControl(CacheControl.noStore()).body(message.getImage());
    }

    @PostMapping("/{kind}/read/{id}")
    @RoleRequired("READER")
    public ChatService.Receipt read(@PathVariable String kind, @PathVariable Long id) {
        return chat.markRead(ChatService.room(kind), id);
    }

    @PutMapping("/{kind}/reactions/{id}")
    @RoleRequired("READER")
    public ChatService.ReactionUpdate reaction(@PathVariable String kind, @PathVariable Long id,
                                               @RequestBody ReactionRequest request) {
        return chat.setReaction(ChatService.room(kind), id, request.emoji());
    }

    public record TextRequest(String text, Long replyToId) {}
    public record TypingRequest(String activityId, boolean typing) {}
    public record PresenceRequest(String sessionId, boolean online) {}
    public record PushRequest(boolean enabled) {}
    public record ReactionRequest(String emoji) {}

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> forbidden(SecurityException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> invalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
