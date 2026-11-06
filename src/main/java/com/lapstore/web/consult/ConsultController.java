package com.lapstore.web.consult;

import java.security.Principal;

import com.lapstore.chat.dto.ChatSessionView;
import com.lapstore.chat.service.ChatService;
import com.lapstore.common.exception.BusinessException;
import com.lapstore.common.web.Text;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** CO-01 danh sách phiên chat · CO-02 khung chat. Tin realtime sẽ đẩy qua WebSocket (/ws) ở bước sau. */
@Controller
@RequestMapping("/staff/consult")
class ConsultController {
    private static final int MAX_LEN = 2000;

    private final ChatService chat;

    ConsultController(ChatService chat) { this.chat = chat; }

    @GetMapping({"", "/"})
    String sessions(Principal principal, Model model) {
        model.addAttribute("sessions", chat.sessions(principal.getName()));
        return "consult/index";
    }

    @GetMapping("/sessions/{id}")
    String open(@PathVariable long id, Model model) {
        ChatSessionView session = chat.session(id);
        model.addAttribute("chat", session);
        model.addAttribute("messages", chat.messages(id));
        return "consult/chat";
    }

    @PostMapping("/sessions/{id}/messages")
    String send(@PathVariable long id, @RequestParam String content, Principal principal) {
        String text = Text.blankToNull(content);
        if (text == null) throw new BusinessException("Tin nhắn không được để trống.");
        if (text.length() > MAX_LEN) throw new BusinessException("Tin nhắn tối đa " + MAX_LEN + " ký tự.");
        chat.staffReply(id, text, principal.getName());
        return "redirect:/staff/consult/sessions/" + id;
    }
}
