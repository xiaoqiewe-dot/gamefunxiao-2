package org.gamefunxiao.game;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

public enum EndHunterPosition {
    MAIN_ISLAND_EDGE("主岛边缘组", Material.END_STONE, 28, 99,
            "主岛边缘开局", "离中央不远，转点更快"),
    CHORUS_SCOUT("紫颂侦察组", Material.CHORUS_FLOWER, 22, 4,
            "外岛紫颂林区域", "有更多紫颂果与拉扯空间"),
    END_CITY_RAIDER("末地城袭击组", Material.END_ROD, 12, 2,
            "靠近末地城路线", "高上限但更稀有"),
    OUTER_RING_MINER("外环采掘组", Material.END_STONE_BRICKS, 18, 4,
            "外岛环线中段", "适合先补方块和杂物"),
    GATEWAY_CHASER("门房追击组", Material.ENDER_EYE, 10, 2,
            "主世界要塞门房通路附近", "更容易第一时间压向猎物"),
    GATEWAY_LOGISTICS("门房后勤组", Material.WHITE_WOOL, 10, 2,
            "主世界要塞门房旁边开局", "自带大量羊毛与木头后勤物资");

    private final String displayName;
    private final Material icon;
    private final int baseWeight;
    private final int maxAssignments;
    private final List<String> description;

    EndHunterPosition(String displayName, Material icon, int baseWeight, int maxAssignments, String... description) {
        this.displayName = displayName;
        this.icon = icon;
        this.baseWeight = baseWeight;
        this.maxAssignments = maxAssignments;
        this.description = Arrays.asList(description);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getBaseWeight() {
        return baseWeight;
    }

    public int getMaxAssignments() {
        return maxAssignments;
    }

    public List<String> getDescription() {
        return description;
    }
}
