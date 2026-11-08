package com.jumbo.trus.service.chat;

/** Ephemeral room event. Each activity has its own ID so concurrent typers do not cancel each other. */
public record ChatTypingEvent(String activityId, long authorId, String author, boolean typing) {}
