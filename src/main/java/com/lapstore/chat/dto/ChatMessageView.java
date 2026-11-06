package com.lapstore.chat.dto;

/** sender: GUEST | STAFF | AI; time đã định dạng. */
public record ChatMessageView(String sender, String content, String time) {}
