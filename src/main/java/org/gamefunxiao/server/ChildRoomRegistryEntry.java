package org.gamefunxiao.server;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.gamefunxiao.game.GameMode;
import org.gamefunxiao.game.RoomState;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChildRoomRegistryEntry {

    private String roomId;
    private UUID ownerUuid;
    private String ownerName;
    private GameMode mode;
    private int maxPlayers;
    private boolean isPublic;
    private Set<String> modifiers = new HashSet<>();
    private String nodeId;
    private String proxyServerName;
    private String host;
    private int port;
    private RoomState state;
    private int currentPlayers;
    private long createdAt;
    private long updatedAt;

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public GameMode getMode() {
        return mode;
    }

    public void setMode(GameMode mode) {
        this.mode = mode;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public Set<String> getModifiers() {
        return new HashSet<>(modifiers);
    }

    public void setModifiers(Set<String> modifiers) {
        this.modifiers = modifiers == null ? new HashSet<>() : new HashSet<>(modifiers);
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getProxyServerName() {
        return proxyServerName;
    }

    public void setProxyServerName(String proxyServerName) {
        this.proxyServerName = proxyServerName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public RoomState getState() {
        return state;
    }

    public void setState(RoomState state) {
        this.state = state;
    }

    public int getCurrentPlayers() {
        return currentPlayers;
    }

    public void setCurrentPlayers(int currentPlayers) {
        this.currentPlayers = currentPlayers;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void save(File file) throws IOException {
        FileConfiguration config = new YamlConfiguration();
        config.set("config_version", 1);
        config.set("room.id", roomId);
        config.set("room.owner_uuid", ownerUuid == null ? null : ownerUuid.toString());
        config.set("room.owner_name", ownerName);
        config.set("room.mode", mode == null ? null : mode.name());
        config.set("room.max_players", maxPlayers);
        config.set("room.public", isPublic);
        config.set("room.modifiers", new ArrayList<>(modifiers));
        config.set("room.state", state == null ? RoomState.WAITING.name() : state.name());
        config.set("room.current_players", currentPlayers);
        config.set("room.created_at", createdAt);
        config.set("room.updated_at", updatedAt > 0L ? updatedAt : System.currentTimeMillis());
        config.set("node.id", nodeId);
        config.set("node.proxy_server_name", proxyServerName);
        config.set("node.host", host);
        config.set("node.port", port);
        config.save(file);
    }

    public static ChildRoomRegistryEntry load(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String roomId = config.getString("room.id");
        String owner = config.getString("room.owner_uuid");
        String modeName = config.getString("room.mode");
        if (roomId == null || owner == null || modeName == null) {
            return null;
        }

        try {
            ChildRoomRegistryEntry entry = new ChildRoomRegistryEntry();
            entry.setRoomId(roomId);
            entry.setOwnerUuid(UUID.fromString(owner));
            entry.setOwnerName(config.getString("room.owner_name", "Unknown"));
            entry.setMode(GameMode.valueOf(modeName));
            entry.setMaxPlayers(config.getInt("room.max_players", 16));
            entry.setPublic(config.getBoolean("room.public", true));
            entry.setModifiers(new HashSet<>(config.getStringList("room.modifiers")));
            entry.setState(RoomState.valueOf(config.getString("room.state", RoomState.WAITING.name())));
            entry.setCurrentPlayers(config.getInt("room.current_players", 0));
            entry.setCreatedAt(config.getLong("room.created_at", System.currentTimeMillis()));
            entry.setUpdatedAt(config.getLong("room.updated_at", entry.getCreatedAt()));
            entry.setNodeId(config.getString("node.id", "node-1"));
            entry.setProxyServerName(config.getString("node.proxy_server_name", "gamefun-node-1"));
            entry.setHost(config.getString("node.host", "127.0.0.1"));
            entry.setPort(config.getInt("node.port", 25580));
            return entry;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
