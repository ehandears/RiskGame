package com.mycompany.riskgameproject.common;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final MessageType type;
    private final String roomCode;
    private final String senderName;
    private final String text;
    private final Serializable payload;

    public Message(MessageType type, String roomCode, String senderName, String text) {
        this(type, roomCode, senderName, text, null);
    }

    public Message(MessageType type, String roomCode, String senderName, String text, Serializable payload) {
        this.type = type;
        this.roomCode = roomCode;
        this.senderName = senderName;
        this.text = text;
        this.payload = payload;
    }

    public MessageType getType() {
        return type;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getText() {
        return text;
    }

    public Serializable getPayload() {
        return payload;
    }
}