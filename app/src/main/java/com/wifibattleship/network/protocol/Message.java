package com.wifibattleship.network.protocol;

import com.google.gson.Gson;

public class Message {

    public enum Type {
        GAME_CONFIG,
        SHIP_PLACEMENT_READY,
        ATTACK,
        ATTACK_RESULT,
        GAME_OVER,
        REMATCH_REQUEST,
        REMATCH_ACCEPT
    }

    private final Type type;
    private final String payload;

    private static final Gson GSON = new Gson();

    public Message(Type type, String payload) {
        this.type = type;
        this.payload = payload != null ? payload : "";
    }

    public Type getType() { return type; }
    public String getPayload() { return payload; }

    public String toJson() { return GSON.toJson(this); }

    public static Message fromJson(String json) { return GSON.fromJson(json, Message.class); }

    public static Message attack(int row, int col) {
        return new Message(Type.ATTACK, row + "," + col);
    }

    public static Message attackResult(int row, int col, String result) {
        return new Message(Type.ATTACK_RESULT, row + "," + col + "," + result);
    }

    public static Message gameConfig(String configJson) {
        return new Message(Type.GAME_CONFIG, configJson);
    }

    /** Extracts [row, col] from an ATTACK payload ("row,col"). */
    public int[] parseAttackCoords() {
        String[] parts = payload.split(",");
        return new int[]{Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
    }
}
