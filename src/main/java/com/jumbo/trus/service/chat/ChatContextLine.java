package com.jumbo.trus.service.chat;

import java.time.Instant;

public record ChatContextLine(String author, String text, Instant createdAt) {}
