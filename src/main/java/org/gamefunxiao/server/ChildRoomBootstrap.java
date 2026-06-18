package org.gamefunxiao.server;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.gamefunxiao.game.GameMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChildRoomBootstrap {

    private final String nodeId;
    private final String proxyServerName;
    private final int port;
    private final String roomId;
    private final UUID ownerUuid;
    private final GameMode mode;
    private final int maxPlayers;
    private final boolean isPublic;
    private final Set<String> modifiers;
    private final long createdAt;

    public ChildRoomBootstrap(String nodeId, String proxyServerName, int port, String roomId, UUID ownerUuid,
                              GameMode mode, int maxPlayers, boolean isPublic, Set<String> modifiers, long createdAt) {
        this.nodeId = nodeId;
        this.proxyServerName = proxyServerName;
        this.port = port;
        this.roomId = roomId;
        this.ownerUuid = ownerUuid;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.isPublic = isPublic;
        this.modifiers = modifiers == null ? new HashSet<>() : new HashSet<>(modifiers);
        this.createdAt = createdAt;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getProxyServerName() {
        return proxyServerName;
    }

    public int getPort() {
        return port;
    }

    public String getRoomId() {
        return roomId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public GameMode getMode() {
        return mode;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public Set<String> getModifiers() {
        return new HashSet<>(modifiers);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void save(File file) throws IOException {
        FileConfiguration config = new YamlConfiguration();
        config.set("config_version", 1);
        config.set("node.id", nodeId);
        config.set("node.proxy_server_name", proxyServerName);
        config.set("node.port", port);
        config.set("room.room_id", roomId);
        config.set("room.owner_uuid", ownerUuid.toString());
        config.set("room.mode", mode.name());
        config.set("room.max_players", maxPlayers);
        config.set("room.public", isPublic);
        config.set("room.modifiers", new ArrayList<>(modifiers));
        config.set("room.created_at", createdAt);
        config.save(file);
    }

    public static ChildRoomBootstrap load(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String roomId = config.getString("room.room_id");
        String owner = config.getString("room.owner_uuid");
        String modeName = config.getString("room.mode");
        if (roomId == null || owner == null || modeName == null) {
            return null;
        }

        try {
            return new ChildRoomBootstrap(
                    config.getString("node.id", "node-1"),
                    config.getString("node.proxy_server_name", "gamefun-node-1"),
                    config.getInt("node.port", 25580),
                    roomId,
                    UUID.fromString(owner),
                    GameMode.valueOf(modeName),
                    config.getInt("room.max_players", 16),
                    config.getBoolean("room.public", true),
                    new HashSet<>(config.getStringList("room.modifiers")),
                    config.getLong("room.created_at", System.currentTimeMillis())
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
