package com.jumbo.trus.controller;

import com.jumbo.trus.entity.chat.ChatMessage;
import com.jumbo.trus.service.chat.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController @RequestMapping("/chat") @RequiredArgsConstructor
public class ChatController {
    private final ChatService chat;

    @GetMapping("/rooms")
    public List<ChatService.Room> rooms() { return chat.rooms(); }

    @PutMapping("/{kind}/push")
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
    public ChatService.Message send(@PathVariable String kind, @RequestBody TextRequest request) throws IOException {
        return chat.send(ChatService.room(kind), request.text(), null, request.replyToId());
    }

    @PostMapping(value = "/{kind}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ChatService.Receipt read(@PathVariable String kind, @PathVariable Long id) {
        return chat.markRead(ChatService.room(kind), id);
    }

    @PutMapping("/{kind}/reactions/{id}")
    public ChatService.ReactionUpdate reaction(@PathVariable String kind, @PathVariable Long id,
                                               @RequestBody ReactionRequest request) {
        return chat.setReaction(ChatService.room(kind), id, request.emoji());
    }

    public record TextRequest(String text, Long replyToId) {}
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
