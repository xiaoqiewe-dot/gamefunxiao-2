package org.gamefunxiao.server;

public enum ChildServerRole {
    STANDALONE,
    LOBBY,
    NODE;

    public static ChildServerRole fromString(String value) {
        if (value == null || value.isBlank()) {
            return STANDALONE;
        }

        return switch (value.trim().toLowerCase()) {
            case "lobby", "master", "controller" -> LOBBY;
            case "node", "arena", "child" -> NODE;
            default -> STANDALONE;
        };
    }
}
