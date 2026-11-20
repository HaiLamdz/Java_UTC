package com.bookstore.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Request gửi tin nhắn hỏi đáp tiếp nối trong khung chat "Trợ lý AI"
 */
public class AiChatRequest {

    private String message;
    private List<ChatTurn> history = new ArrayList<>();

    public AiChatRequest() {
    }

    public static class ChatTurn {
        private String role; // "user" hoặc "assistant"
        private String content;

        public ChatTurn() {
        }

        public ChatTurn(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatTurn> getHistory() {
        return history;
    }

    public void setHistory(List<ChatTurn> history) {
        this.history = history;
    }
}

