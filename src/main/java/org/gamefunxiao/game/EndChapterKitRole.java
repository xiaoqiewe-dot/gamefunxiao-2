package org.gamefunxiao.game;

public enum EndChapterKitRole {

    PREY("prey", "猎物"),
    HUNTER("hunter", "猎人");

    private final String id;
    private final String displayName;

    EndChapterKitRole(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
