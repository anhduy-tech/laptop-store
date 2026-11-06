package com.lapstore.chat.service;

import java.util.List;

import com.lapstore.chat.dto.ChatMessageView;
import com.lapstore.chat.dto.ChatSessionRow;
import com.lapstore.chat.dto.ChatSessionView;

/** Tư vấn trực tuyến (CO-01, CO-02). Sau này đẩy tin mới qua WebSocket (/ws). */
public interface ChatService {
    List<ChatSessionRow> sessions(String username);

    /** @throws com.lapstore.common.exception.NotFoundException */
    ChatSessionView session(long sessionId);

    List<ChatMessageView> messages(long sessionId);

    void staffReply(long sessionId, String content, String username);
}
