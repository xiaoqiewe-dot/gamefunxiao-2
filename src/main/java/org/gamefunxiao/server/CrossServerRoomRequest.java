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

public class CrossServerRoomRequest {

    private final UUID ownerUuid;
    private final String ownerName;
    private final String roomId;
    private final GameMode mode;
    private final int maxPlayers;
    private final boolean publicRoom;
    private final Set<String> modifiers;
    private final String lobbyServerName;
    private final long createdAt;

    public CrossServerRoomRequest(UUID ownerUuid, String ownerName, String roomId, GameMode mode,
                                  int maxPlayers, boolean publicRoom, Set<String> modifiers,
                                  String lobbyServerName, long createdAt) {
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.roomId = roomId;
        this.mode = mode;
        this.maxPlayers = maxPlayers;
        this.publicRoom = publicRoom;
        this.modifiers = modifiers == null ? new HashSet<>() : new HashSet<>(modifiers);
        this.lobbyServerName = lobbyServerName;
        this.createdAt = createdAt;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public String getRoomId() {
        return roomId;
    }

    public GameMode getMode() {
        return mode;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public boolean isPublicRoom() {
        return publicRoom;
    }

    public Set<String> getModifiers() {
        return new HashSet<>(modifiers);
    }

    public String getLobbyServerName() {
        return lobbyServerName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void save(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        FileConfiguration config = new YamlConfiguration();
        config.set("config_version", 1);
        config.set("owner.uuid", ownerUuid.toString());
        config.set("owner.name", ownerName);
        config.set("room.id", roomId);
        config.set("room.mode", mode.name());
        config.set("room.max_players", maxPlayers);
        config.set("room.public", publicRoom);
        config.set("room.modifiers", new ArrayList<>(modifiers));
        config.set("server.lobby", lobbyServerName);
        config.set("created_at", createdAt);
        config.save(file);
    }

    public static CrossServerRoomRequest load(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String owner = config.getString("owner.uuid");
        String roomId = config.getString("room.id");
        String modeName = config.getString("room.mode");
        if (owner == null || roomId == null || modeName == null) {
            return null;
        }

        try {
            return new CrossServerRoomRequest(
                    UUID.fromString(owner),
                    config.getString("owner.name", "Unknown"),
                    roomId,
                    GameMode.valueOf(modeName),
                    config.getInt("room.max_players", 16),
                    config.getBoolean("room.public", true),
                    new HashSet<>(config.getStringList("room.modifiers")),
                    config.getString("server.lobby", "lobby"),
                    config.getLong("created_at", System.currentTimeMillis())
            );
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
