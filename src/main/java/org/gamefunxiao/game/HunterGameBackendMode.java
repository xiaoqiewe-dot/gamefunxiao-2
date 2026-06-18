package org.gamefunxiao.game;

public enum HunterGameBackendMode {
    CURRENT_SERVER_WORLD,
    CHILD_SERVER,
    CROSS_SERVER;

    public static HunterGameBackendMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return CURRENT_SERVER_WORLD;
        }

        return switch (value.trim().toLowerCase()) {
            case "child_server", "child", "subserver", "sub_server", "node", "lobby" -> CHILD_SERVER;
            case "cross_server", "crossserver", "remote_server", "server_transfer", "transfer", "bungee", "velocity" -> CROSS_SERVER;
            case "current", "current_server_world", "world", "standalone", "local" -> CURRENT_SERVER_WORLD;
            default -> CROSS_SERVER;
        };
    }
}
